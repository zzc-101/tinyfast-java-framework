package com.zc.base.security.service;

import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.common.util.RedisCache;
import com.zc.base.common.util.ServletUtil;
import com.zc.base.security.dto.LoginRequest;
import com.zc.base.security.dto.LoginResponse;
import com.zc.base.security.dto.RefreshTokenRequest;
import com.zc.base.security.event.LoginEvent;
import com.zc.base.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ONLINE_USER_PREFIX = "online:user:";

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SecurityProperties securityProperties;
    private final RedisCache redisCache;
    private final TokenRevokeService tokenRevokeService;
    private final UserDetailsService userDetailsService;
    private final ApplicationEventPublisher eventPublisher;
    private final CaptchaService captchaService;
    private final LoginAttemptService loginAttemptService;

    /**
     * 登录，成功和失败都会发布登录事件，由日志模块记录。
     * 顺序：验证码 -> 锁定检查 -> 密码。
     */
    public LoginResponse login(LoginRequest request) {
        try {
            captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
            loginAttemptService.checkLocked(request.getUsername());
        } catch (BusinessException e) {
            publishLoginEvent(request.getUsername(), false, e.getMessage());
            throw e;
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            // 用户名不存在也会走到这里（Spring Security 默认把它转成 BadCredentialsException），计数和提示与密码错误一致
            if (loginAttemptService.recordFailure(request.getUsername())) {
                String message = loginAttemptService.lockedMessage();
                publishLoginEvent(request.getUsername(), false, message);
                throw new BusinessException(429, message);
            }
            publishLoginEvent(request.getUsername(), false, failureMessage(e));
            throw e;
        } catch (AuthenticationException e) {
            publishLoginEvent(request.getUsername(), false, failureMessage(e));
            throw e;
        }
        loginAttemptService.clearFailures(request.getUsername());

        SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getRoles());

        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtUtil.createAccessToken(userDetails.getUserId(), userDetails.getUsername(), sessionId, claims);
        String refreshToken = jwtUtil.createRefreshToken(userDetails.getUserId(), userDetails.getUsername(), sessionId);

        // 记录在线用户
        long expireMinutes = securityProperties.getJwt().getAccessTokenExpire();
        Map<String, Object> onlineInfo = new HashMap<>();
        onlineInfo.put("userId", userDetails.getUserId());
        onlineInfo.put("username", userDetails.getUsername());
        onlineInfo.put("ip", ServletUtil.getClientIp());
        onlineInfo.put("loginTime", System.currentTimeMillis());
        redisCache.set(ONLINE_USER_PREFIX + userDetails.getUserId(), onlineInfo, expireMinutes, TimeUnit.MINUTES);

        publishLoginEvent(userDetails.getUsername(), true, "登录成功");

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expireMinutes * 60)
                .build();
    }

    /**
     * 刷新token
     */
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        Claims claims = jwtUtil.parseToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException(401, "refreshToken无效或已过期"));

        String type = claims.get(JwtUtil.CLAIM_TYPE, String.class);
        if (!JwtUtil.TYPE_REFRESH.equals(type)) {
            throw new BusinessException(401, "无效的refreshToken");
        }
        if (tokenRevokeService.isRevoked(claims)) {
            throw new BusinessException(401, "refreshToken无效或已过期");
        }

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);

        // 重新加载用户：已删除或已禁用的用户不能再换新 token
        UserDetails loaded;
        try {
            loaded = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new BusinessException(401, "用户不存在");
        }
        if (!loaded.isEnabled()) {
            throw new BusinessException(401, "账号已被禁用");
        }
        SecurityUserDetails userDetails = (SecurityUserDetails) loaded;

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", userDetails.getRoles());

        String sessionId = claims.get(JwtUtil.CLAIM_SESSION_ID, String.class);
        String accessToken = jwtUtil.createAccessToken(userId, username, sessionId, extraClaims);
        String refreshToken = jwtUtil.createRefreshToken(userId, username, sessionId);

        long expireMinutes = securityProperties.getJwt().getAccessTokenExpire();
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expireMinutes * 60)
                .build();
    }

    /**
     * 登出
     */
    public void logout(String token) {
        jwtUtil.parseToken(token).ifPresent(claims -> {
            Long userId = Long.valueOf(claims.getSubject());
            // 吊销本次会话，access token 和 refresh token 一并失效
            tokenRevokeService.revokeSession(claims);
            // 移除在线用户
            redisCache.delete(ONLINE_USER_PREFIX + userId);
        });
    }

    private void publishLoginEvent(String username, boolean success, String message) {
        eventPublisher.publishEvent(new LoginEvent(
                username,
                ServletUtil.getClientIp(),
                ServletUtil.getRequest().map(request -> request.getHeader("User-Agent")).orElse(""),
                success,
                message));
    }

    private String failureMessage(AuthenticationException e) {
        if (e instanceof BadCredentialsException) {
            return "用户名或密码错误";
        }
        if (e instanceof AccountStatusException) {
            return "账号已被禁用";
        }
        return "登录失败";
    }

}

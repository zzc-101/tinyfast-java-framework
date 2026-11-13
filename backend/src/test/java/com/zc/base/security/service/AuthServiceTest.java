package com.zc.base.security.service;

import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.common.util.RedisCache;
import com.zc.base.security.dto.LoginRequest;
import com.zc.base.security.dto.LoginResponse;
import com.zc.base.security.dto.RefreshTokenRequest;
import com.zc.base.security.event.LoginEvent;
import com.zc.base.security.util.JwtUtil;
import com.zc.base.system.entity.SysUser;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "tester";

    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private AuthenticationManager authenticationManager;
    private ApplicationEventPublisher eventPublisher;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setSecret("test-secret-key-at-least-32-characters-long!!");
        jwtUtil = new JwtUtil(properties);

        RedisCache redisCache = mock(RedisCache.class);
        when(redisCache.hasKey(any())).thenReturn(false);
        when(redisCache.get(any())).thenReturn(java.util.Optional.empty());

        userDetailsService = mock(UserDetailsService.class);
        authenticationManager = mock(AuthenticationManager.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        authService = new AuthService(authenticationManager, jwtUtil, properties, redisCache,
                new TokenRevokeService(redisCache, properties), userDetailsService, eventPublisher,
                mock(CaptchaService.class), mock(LoginAttemptService.class));
    }

    @Test
    void refreshWithEnabledUserIssuesNewTokensWithRoles() {
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(user(1, Set.of("ADMIN")));

        LoginResponse response = authService.refreshToken(refreshRequest());

        Claims claims = jwtUtil.parseToken(response.getAccessToken()).orElseThrow();
        assertEquals(JwtUtil.TYPE_ACCESS, claims.get(JwtUtil.CLAIM_TYPE, String.class));
        assertEquals("s1", claims.get(JwtUtil.CLAIM_SESSION_ID, String.class));
        assertEquals(java.util.List.of("ADMIN"), claims.get("roles", java.util.List.class));
    }

    @Test
    void refreshWithDisabledUserReturns401() {
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(user(0, Set.of("ADMIN")));

        BusinessException e = assertThrows(BusinessException.class, () -> authService.refreshToken(refreshRequest()));
        assertEquals(401, e.getCode());
    }

    @Test
    void refreshWithDeletedUserReturns401() {
        when(userDetailsService.loadUserByUsername(USERNAME)).thenThrow(new UsernameNotFoundException(USERNAME));

        BusinessException e = assertThrows(BusinessException.class, () -> authService.refreshToken(refreshRequest()));
        assertEquals(401, e.getCode());
    }

    @Test
    void accessTokenCannotBeUsedToRefresh() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(jwtUtil.createAccessToken(USER_ID, USERNAME, "s1", java.util.Map.of()));

        BusinessException e = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(401, e.getCode());
    }

    @Test
    void failedLoginPublishesFailureEvent() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest()));

        LoginEvent event = capturedEvent();
        assertFalse(event.success());
        assertEquals(USERNAME, event.username());
        assertEquals("用户名或密码错误", event.message());
    }

    @Test
    void disabledAccountLoginPublishesFailureEvent() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThrows(DisabledException.class, () -> authService.login(loginRequest()));

        assertEquals("账号已被禁用", capturedEvent().message());
    }

    private LoginEvent capturedEvent() {
        ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword("x");
        return request;
    }

    private RefreshTokenRequest refreshRequest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(jwtUtil.createRefreshToken(USER_ID, USERNAME, "s1"));
        return request;
    }

    private SecurityUserDetails user(int status, Set<String> roles) {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setUsername(USERNAME);
        user.setStatus(status);
        return new SecurityUserDetails(user, roles, Set.of());
    }
}

package com.zc.base.security.filter;

import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.security.service.CachedUserDetailsService;
import com.zc.base.security.service.TokenRevokeService;
import com.zc.base.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * JWT 认证过滤器
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final CachedUserDetailsService cachedUserDetailsService;
    private final SecurityProperties securityProperties;
    private final TokenRevokeService tokenRevokeService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 检查是否为免认证路径
        if (isIgnoredUrl(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtUtil.parseToken(token)
                    .filter(claims -> JwtUtil.TYPE_ACCESS.equals(claims.get(JwtUtil.CLAIM_TYPE, String.class)))
                    .filter(claims -> !tokenRevokeService.isRevoked(claims))
                    .flatMap(this::loadEnabledUser)
                    .ifPresent(userDetails -> {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(TOKEN_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 从缓存（未命中时查库）加载用户；用户已删除或已禁用时不认证，交给后续的未登录处理返回 401
     */
    private Optional<UserDetails> loadEnabledUser(Claims claims) {
        String username = claims.get("username", String.class);
        if (username == null) {
            return Optional.empty();
        }
        // 用户名可能在改名后被别的用户复用，必须核对用户 ID
        return cachedUserDetailsService.load(username)
                .filter(user -> String.valueOf(user.getUserId()).equals(claims.getSubject()))
                .map(UserDetails.class::cast)
                .filter(UserDetails::isEnabled);
    }

    private boolean isIgnoredUrl(String requestUri) {
        return securityProperties.getIgnoreUrls().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }

}

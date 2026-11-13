package com.zc.base.security.filter;

import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.util.RedisCache;
import com.zc.base.security.service.CachedUserDetailsService;
import com.zc.base.security.service.SecurityUserDetails;
import com.zc.base.security.service.TokenRevokeService;
import com.zc.base.security.util.JwtUtil;
import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.entity.SysUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "admin";

    private final Map<String, Object> redis = new HashMap<>();
    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private TokenRevokeService tokenRevokeService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setSecret("test-secret-key-at-least-32-characters-long!!");
        jwtUtil = new JwtUtil(properties);

        RedisCache redisCache = mock(RedisCache.class);
        doAnswer(inv -> redis.put(inv.getArgument(0), inv.getArgument(1)))
                .when(redisCache).set(anyString(), any(), anyLong(), any());
        when(redisCache.hasKey(anyString())).thenAnswer(inv -> redis.containsKey(inv.<String>getArgument(0)));
        when(redisCache.get(anyString())).thenAnswer(inv -> Optional.ofNullable(redis.get(inv.<String>getArgument(0))));

        userDetailsService = mock(UserDetailsService.class);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(user(1));

        tokenRevokeService = new TokenRevokeService(redisCache, properties);
        // RedisCache 是 mock，用户缓存始终未命中，这里只验证过滤器的认证逻辑；缓存本身见 AuthUserCacheTest
        CachedUserDetailsService cachedUserDetailsService =
                new CachedUserDetailsService(new AuthUserCache(redisCache, properties), userDetailsService);
        filter = new JwtAuthenticationFilter(jwtUtil, cachedUserDetailsService, properties, tokenRevokeService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accessTokenAuthenticates() throws Exception {
        assertNotNull(authenticate(accessToken()));
    }

    @Test
    void refreshTokenIsNotAcceptedAsAccessToken() throws Exception {
        assertNull(authenticate(jwtUtil.createRefreshToken(USER_ID, USERNAME, "s1")));
    }

    @Test
    void disabledUserIsRejected() throws Exception {
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(user(0));
        assertNull(authenticate(accessToken()));
    }

    @Test
    void deletedUserIsRejectedWithoutError() throws Exception {
        when(userDetailsService.loadUserByUsername(USERNAME)).thenThrow(new UsernameNotFoundException(USERNAME));
        assertNull(authenticate(accessToken()));
    }

    @Test
    void tokenOfAnotherUserWithSameUsernameIsRejected() throws Exception {
        assertNull(authenticate(jwtUtil.createAccessToken(USER_ID + 1, USERNAME, "s1", Map.of())));
    }

    @Test
    void loggedOutSessionIsRejected() throws Exception {
        String token = accessToken();
        tokenRevokeService.revokeSession(jwtUtil.parseToken(token).orElseThrow());
        assertNull(authenticate(token));
    }

    @Test
    void forceLogoutRejectsTokensIssuedBefore() throws Exception {
        String token = accessToken();
        tokenRevokeService.revokeUser(USER_ID);
        assertNull(authenticate(token));
    }

    @Test
    void tokenIssuedAfterForceLogoutAuthenticates() throws Exception {
        redis.put("token:revoke:user:" + USER_ID, System.currentTimeMillis() - 5000);
        assertNotNull(authenticate(accessToken()));
    }

    private String accessToken() {
        return jwtUtil.createAccessToken(USER_ID, USERNAME, "s1", Map.of());
    }

    private Authentication authenticate(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/users");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private SecurityUserDetails user(int status) {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setUsername(USERNAME);
        user.setStatus(status);
        return new SecurityUserDetails(user, Set.of("ADMIN"), Set.of());
    }
}

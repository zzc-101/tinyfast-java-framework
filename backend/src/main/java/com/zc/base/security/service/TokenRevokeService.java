package com.zc.base.security.service;

import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.util.RedisCache;
import com.zc.base.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 吊销：登出吊销单个会话，强退吊销用户的全部会话
 */
@Service
@RequiredArgsConstructor
public class TokenRevokeService {

    private static final String SESSION_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String USER_REVOKE_PREFIX = "token:revoke:user:";

    private final RedisCache redisCache;
    private final SecurityProperties securityProperties;

    /**
     * 吊销 token 所属会话（同一次登录签发及后续刷新出的 access/refresh token）
     */
    public void revokeSession(Claims claims) {
        redisCache.set(SESSION_BLACKLIST_PREFIX + sessionId(claims), "1", refreshTokenExpireDays(), TimeUnit.DAYS);
    }

    /**
     * 吊销用户此刻之前签发的全部 token
     */
    public void revokeUser(Long userId) {
        redisCache.set(USER_REVOKE_PREFIX + userId, System.currentTimeMillis(), refreshTokenExpireDays(), TimeUnit.DAYS);
    }

    public boolean isRevoked(Claims claims) {
        String sessionId = sessionId(claims);
        if (sessionId == null || redisCache.hasKey(SESSION_BLACKLIST_PREFIX + sessionId)) {
            return true;
        }
        // JWT 的签发时间只精确到秒，与吊销发生在同一秒的 token 一律视为已吊销
        return redisCache.<Number>get(USER_REVOKE_PREFIX + claims.getSubject())
                .map(revokedAt -> claims.getIssuedAt().getTime() <= revokedAt.longValue())
                .orElse(false);
    }

    private String sessionId(Claims claims) {
        return claims.get(JwtUtil.CLAIM_SESSION_ID, String.class);
    }

    private long refreshTokenExpireDays() {
        return securityProperties.getJwt().getRefreshTokenExpire();
    }
}

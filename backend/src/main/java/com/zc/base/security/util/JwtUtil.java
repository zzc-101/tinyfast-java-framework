package com.zc.base.security.util;

import com.zc.base.common.config.properties.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JWT 工具类
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_SESSION_ID = "sid";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecurityProperties securityProperties;

    /**
     * 生成 access_token
     */
    public String createAccessToken(Long userId, String username, String sessionId, Map<String, Object> extraClaims) {
        long expireMillis = securityProperties.getJwt().getAccessTokenExpire() * 60 * 1000L;
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(CLAIM_TYPE, TYPE_ACCESS);
        claims.put(CLAIM_SESSION_ID, sessionId);
        return createToken(userId, username, claims, expireMillis);
    }

    /**
     * 生成 refresh_token
     */
    public String createRefreshToken(Long userId, String username, String sessionId) {
        long expireMillis = securityProperties.getJwt().getRefreshTokenExpire() * 24 * 60 * 60 * 1000L;
        return createToken(userId, username, Map.of(CLAIM_TYPE, TYPE_REFRESH, CLAIM_SESSION_ID, sessionId), expireMillis);
    }

    private String createToken(Long userId, String username, Map<String, Object> extraClaims, long expireMillis) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claims(extraClaims)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析token
     */
    public Optional<Claims> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 从token中获取用户ID
     */
    public Optional<Long> getUserId(String token) {
        return parseToken(token).map(claims -> Long.valueOf(claims.getSubject()));
    }

    /**
     * 判断token是否过期
     */
    public boolean isTokenExpired(String token) {
        return parseToken(token)
                .map(claims -> claims.getExpiration().before(new Date()))
                .orElse(true);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = securityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}

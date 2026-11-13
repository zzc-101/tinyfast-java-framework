package com.zc.base.security.util;

import com.zc.base.security.service.SecurityUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 安全上下文工具类 - 获取当前登录用户信息
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * 获取当前登录用户
     */
    public static Optional<SecurityUserDetails> getCurrentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(SecurityUserDetails.class::isInstance)
                .map(SecurityUserDetails.class::cast);
    }

    /**
     * 获取当前用户ID
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(SecurityUserDetails::getUserId);
    }

    /**
     * 获取当前用户名
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentUser().map(SecurityUserDetails::getUsername);
    }

}

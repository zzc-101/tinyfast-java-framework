package com.zc.base.security.service;

import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.cache.AuthUserInfo;
import com.zc.base.system.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;

/**
 * 鉴权路径用的用户加载：先读 Redis 缓存，未命中再查库。
 * 取出的 SecurityUserDetails 不含密码，只用于 JWT 认证；登录仍走 UserDetailsServiceImpl 直接查库。
 */
@Service
@RequiredArgsConstructor
public class CachedUserDetailsService {

    private final AuthUserCache authUserCache;
    private final UserDetailsService userDetailsService;

    public Optional<SecurityUserDetails> load(String username) {
        return authUserCache.get(username, () -> loadFromDb(username)).map(CachedUserDetailsService::toDetails);
    }

    private Optional<AuthUserInfo> loadFromDb(String username) {
        try {
            return Optional.of(toInfo((SecurityUserDetails) userDetailsService.loadUserByUsername(username)));
        } catch (UsernameNotFoundException e) {
            return Optional.empty();
        }
    }

    static AuthUserInfo toInfo(SecurityUserDetails details) {
        AuthUserInfo info = new AuthUserInfo();
        info.setUserId(details.getUserId());
        info.setUsername(details.getUsername());
        info.setNickname(details.getNickname());
        info.setStatus(details.getStatus());
        info.setRoles(new LinkedHashSet<>(details.getRoles()));
        info.setPermissions(new LinkedHashSet<>(details.getPermissions()));
        info.setRoleIds(new ArrayList<>(details.getRoleIds()));
        return info;
    }

    static SecurityUserDetails toDetails(AuthUserInfo info) {
        SysUser user = new SysUser();
        user.setId(info.getUserId());
        user.setUsername(info.getUsername());
        user.setNickname(info.getNickname());
        user.setStatus(info.getStatus());
        SecurityUserDetails details = new SecurityUserDetails(user, info.getRoles(), info.getPermissions());
        details.setRoleIds(info.getRoleIds());
        return details;
    }

}

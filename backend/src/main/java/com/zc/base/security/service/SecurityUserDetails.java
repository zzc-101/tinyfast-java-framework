package com.zc.base.security.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zc.base.system.entity.SysUser;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security 用户详情
 */
@Data
public class SecurityUserDetails implements UserDetails {

    /** 拥有该角色标识的用户是超级管理员，拥有全部权限 */
    public static final String ADMIN_ROLE = "ADMIN";

    private Long userId;
    private String username;
    @JsonIgnore
    private String password;
    private String nickname;
    private Integer status;

    /** 角色标识集合 */
    private Set<String> roles;

    /** 权限标识集合，格式 模块:资源:操作 */
    private Set<String> permissions;

    /** 有效（未禁用）角色的ID */
    @JsonIgnore
    private List<Long> roleIds = List.of();

    @JsonIgnore
    private List<SimpleGrantedAuthority> authorities;

    public SecurityUserDetails(SysUser user, Set<String> roles, Set<String> permissions) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.nickname = user.getNickname();
        this.status = user.getStatus();
        this.roles = roles;
        this.permissions = permissions;
    }

    @JsonIgnore
    public boolean isAdmin() {
        return roles.contains(ADMIN_ROLE);
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities == null) {
            authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
            // 附加权限标识
            if (permissions != null) {
                permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }
        }
        return authorities;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return status != null && status == 1;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return status != null && status == 1;
    }

}

package com.zc.base.security.service;

import com.zc.base.system.entity.SysRole;
import com.zc.base.system.entity.SysUser;
import com.zc.base.system.entity.SysUserRole;
import com.zc.base.system.mapper.SysRoleMapper;
import com.zc.base.system.mapper.SysUserMapper;
import com.zc.base.system.mapper.SysUserRoleMapper;
import com.zc.base.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zc.base.system.entity.table.SysRoleTableDef.SYS_ROLE;
import static com.zc.base.system.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;
import static com.zc.base.system.entity.table.SysUserTableDef.SYS_USER;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuService menuService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOneByCondition(SYS_USER.USERNAME.eq(username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        List<SysRole> roles = getUserRoles(user.getId());
        Set<String> roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();

        // ADMIN 角色是超级管理员，直接拿全部菜单的权限标识；其他角色只拿分配到的
        boolean admin = roleKeys.contains(SecurityUserDetails.ADMIN_ROLE);
        Set<String> permissions = menuService.listPermissions(admin, roleIds);

        SecurityUserDetails details = new SecurityUserDetails(user, roleKeys, permissions);
        details.setRoleIds(roleIds);
        return details;
    }

    /**
     * 获取用户的有效（未禁用）角色
     */
    private List<SysRole> getUserRoles(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectListByCondition(
                SYS_USER_ROLE.USER_ID.eq(userId));
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .toList();
        return roleMapper.selectListByCondition(
                SYS_ROLE.ID.in(roleIds).and(SYS_ROLE.STATUS.eq(1)));
    }

}

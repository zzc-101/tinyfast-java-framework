package com.zc.base.security.service;

import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.entity.SysMenu;
import com.zc.base.system.entity.SysRole;
import com.zc.base.system.entity.SysUser;
import com.zc.base.system.entity.SysUserRole;
import com.zc.base.system.mapper.SysMenuMapper;
import com.zc.base.system.mapper.SysRoleMapper;
import com.zc.base.system.mapper.SysRoleMenuMapper;
import com.zc.base.system.mapper.SysUserMapper;
import com.zc.base.system.mapper.SysUserRoleMapper;
import com.zc.base.system.service.SysMenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    private static final Long USER_ID = 10L;

    private SysUserMapper userMapper;
    private SysRoleMapper roleMapper;
    private SysUserRoleMapper userRoleMapper;
    private SysMenuMapper menuMapper;
    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        menuMapper = mock(SysMenuMapper.class);
        service = new UserDetailsServiceImpl(userMapper, roleMapper, userRoleMapper,
                new SysMenuService(menuMapper, mock(SysRoleMenuMapper.class), mock(AuthUserCache.class)));

        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setUsername("tester");
        user.setStatus(1);
        when(userMapper.selectOneByCondition(any(QueryCondition.class))).thenReturn(user);
    }

    @Test
    void adminGetsAllPermissionsWithoutRoleAssignment() {
        givenRole(1L, "ADMIN");
        // 全部启用的菜单（含未分配给任何角色的）
        when(menuMapper.selectListByCondition(any(QueryCondition.class))).thenReturn(List.of(
                menu(1L, 0L, "M", ""),
                menu(2L, 1L, "B", "system:user:list"),
                menu(3L, 1L, "B", "system:user:delete"),
                menu(4L, 1L, "B", "log:login:clear")));

        SecurityUserDetails details = (SecurityUserDetails) service.loadUserByUsername("tester");

        assertTrue(details.isAdmin());
        assertEquals(Set.of("system:user:list", "system:user:delete", "log:login:clear"), details.getPermissions());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("system:user:delete")));
        verify(menuMapper, never()).selectListByQuery(any(QueryWrapper.class));
    }

    @Test
    void normalRoleOnlyGetsAssignedPermissions() {
        givenRole(2L, "USER");
        // 数据库按角色分配过滤后只返回这两项
        when(menuMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(
                menu(1L, 0L, "M", ""),
                menu(2L, 1L, "B", "system:user:list")));

        SecurityUserDetails details = (SecurityUserDetails) service.loadUserByUsername("tester");

        assertEquals(Set.of("USER"), details.getRoles());
        assertEquals(Set.of("system:user:list"), details.getPermissions());
        verify(menuMapper, never()).selectListByCondition(any(QueryCondition.class));
    }

    @Test
    void userWithoutRolesGetsNoPermissions() {
        when(userRoleMapper.selectListByCondition(any(QueryCondition.class))).thenReturn(List.of());

        SecurityUserDetails details = (SecurityUserDetails) service.loadUserByUsername("tester");

        assertTrue(details.getRoles().isEmpty());
        assertTrue(details.getPermissions().isEmpty());
    }

    @Test
    void missingUserThrows() {
        when(userMapper.selectOneByCondition(any(QueryCondition.class))).thenReturn(null);
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ghost"));
    }

    private void givenRole(Long roleId, String roleKey) {
        when(userRoleMapper.selectListByCondition(any(QueryCondition.class)))
                .thenReturn(List.of(new SysUserRole(USER_ID, roleId)));
        SysRole role = new SysRole();
        role.setId(roleId);
        role.setRoleKey(roleKey);
        role.setStatus(1);
        when(roleMapper.selectListByCondition(any(QueryCondition.class))).thenReturn(List.of(role));
    }

    private SysMenu menu(Long id, Long parentId, String type, String permission) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuType(type);
        menu.setPermission(permission);
        menu.setStatus(1);
        return menu;
    }
}

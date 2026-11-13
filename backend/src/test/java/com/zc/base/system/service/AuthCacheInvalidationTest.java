package com.zc.base.system.service;

import com.mybatisflex.core.query.QueryCondition;
import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.util.InMemoryRedisCache;
import com.zc.base.security.service.TokenRevokeService;
import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.cache.AuthUserInfo;
import com.zc.base.system.dto.SysMenuDTO;
import com.zc.base.system.dto.SysRoleDTO;
import com.zc.base.system.dto.SysUserDTO;
import com.zc.base.system.entity.SysMenu;
import com.zc.base.system.entity.SysRole;
import com.zc.base.system.entity.SysUser;
import com.zc.base.system.mapper.SysMenuMapper;
import com.zc.base.system.mapper.SysRoleMapper;
import com.zc.base.system.mapper.SysRoleMenuMapper;
import com.zc.base.system.mapper.SysUserMapper;
import com.zc.base.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户、角色、菜单的写操作之后，鉴权用户缓存必须失效。缓存用真实的 AuthUserCache（Redis 为内存版）
 */
class AuthCacheInvalidationTest {

    private final AtomicInteger dbReads = new AtomicInteger();
    private AuthUserCache cache;
    private SysUserMapper userMapper;
    private SysUserRoleMapper userRoleMapper;
    private SysRoleMapper roleMapper;
    private SysRoleMenuMapper roleMenuMapper;
    private SysMenuMapper menuMapper;
    private TokenRevokeService tokenRevokeService;
    private SysUserService userService;
    private SysRoleService roleService;
    private SysMenuService menuService;

    @BeforeEach
    void setUp() {
        cache = new AuthUserCache(new InMemoryRedisCache(), new SecurityProperties());
        userMapper = mock(SysUserMapper.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        roleMenuMapper = mock(SysRoleMenuMapper.class);
        menuMapper = mock(SysMenuMapper.class);
        tokenRevokeService = mock(TokenRevokeService.class);

        userService = new SysUserService(userMapper, userRoleMapper, new BCryptPasswordEncoder(4), cache, tokenRevokeService);
        roleService = new SysRoleService(roleMapper, roleMenuMapper, userRoleMapper, cache);
        menuService = new SysMenuService(menuMapper, roleMenuMapper, cache);

        warm("alice");
        warm("bob");
    }

    @Test
    void updateUserEvictsThatUser() {
        when(userMapper.selectOneById(1L)).thenReturn(user(1L, "alice"));
        SysUserDTO dto = new SysUserDTO();
        dto.setUsername("alice");
        dto.setStatus(0);
        dto.setRoleIds(List.of(2L));

        userService.update(1L, dto);

        assertEquals(true, isEvicted("alice"));
        assertEquals(false, isEvicted("bob"));
    }

    @Test
    void renameUserEvictsOldAndNewUsername() {
        when(userMapper.selectOneById(1L)).thenReturn(user(1L, "alice"));
        warm("alice2");
        SysUserDTO dto = new SysUserDTO();
        dto.setUsername("alice2");

        userService.update(1L, dto);

        assertEquals(true, isEvicted("alice"));
        assertEquals(true, isEvicted("alice2"));
        assertEquals(false, isEvicted("bob"));
    }

    @Test
    void deleteUserEvictsThatUser() {
        when(userMapper.selectOneById(1L)).thenReturn(user(1L, "alice"));

        userService.delete(1L);

        assertEquals(true, isEvicted("alice"));
        assertEquals(false, isEvicted("bob"));
    }

    @Test
    void resetPasswordEvictsThatUserAndRevokesTokens() {
        when(userMapper.selectOneById(1L)).thenReturn(user(1L, "alice"));

        userService.resetPassword(1L, "newpass123");

        assertEquals(true, isEvicted("alice"));
        assertEquals(false, isEvicted("bob"));
        verify(tokenRevokeService).revokeUser(1L);
    }

    @Test
    void resetPasswordOfMissingUserDoesNothing() {
        assertThrows(RuntimeException.class, () -> userService.resetPassword(99L, "newpass123"));
        verify(tokenRevokeService, never()).revokeUser(any());
    }

    @Test
    void updateRoleEvictsAllUsers() {
        SysRole role = new SysRole();
        role.setId(5L);
        role.setRoleKey("OPS");
        when(roleMapper.selectOneById(5L)).thenReturn(role);
        SysRoleDTO dto = new SysRoleDTO();
        dto.setRoleKey("OPS");
        dto.setRoleName("运营");
        dto.setStatus(0);
        dto.setMenuIds(List.of(1L, 2L));

        roleService.update(5L, dto);

        assertEquals(true, isEvicted("alice"));
        assertEquals(true, isEvicted("bob"));
    }

    @Test
    void deleteRoleEvictsAllUsers() {
        when(userRoleMapper.selectCountByCondition(any(QueryCondition.class))).thenReturn(0L);

        roleService.delete(5L);

        assertEquals(true, isEvicted("alice"));
        assertEquals(true, isEvicted("bob"));
    }

    @Test
    void roleDeleteRejectedWhenInUseKeepsCache() {
        when(userRoleMapper.selectCountByCondition(any(QueryCondition.class))).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> roleService.delete(5L));

        assertEquals(false, isEvicted("alice"));
    }

    @Test
    void createUpdateDeleteMenuEvictAllUsers() {
        SysMenuDTO dto = new SysMenuDTO();
        dto.setMenuName("新菜单");
        dto.setMenuType("M");

        menuService.create(dto);
        assertEquals(true, isEvicted("alice"));
        assertEquals(true, isEvicted("bob"));

        SysMenu existing = new SysMenu();
        existing.setId(8L);
        when(menuMapper.selectOneById(8L)).thenReturn(existing);
        warm("alice");
        menuService.update(8L, dto);
        assertEquals(true, isEvicted("alice"));

        when(menuMapper.selectCountByCondition(any(QueryCondition.class))).thenReturn(0L);
        warm("alice");
        menuService.delete(8L);
        assertEquals(true, isEvicted("alice"));
    }

    private SysUser user(Long id, String username) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(1);
        return user;
    }

    private void warm(String username) {
        cache.get(username, () -> Optional.of(info(username)));
    }

    /** 缓存里没有该用户了：再读一次会走 loader */
    private boolean isEvicted(String username) {
        dbReads.set(0);
        cache.get(username, () -> {
            dbReads.incrementAndGet();
            return Optional.of(info(username));
        });
        return dbReads.get() == 1;
    }

    private AuthUserInfo info(String username) {
        AuthUserInfo info = new AuthUserInfo();
        info.setUsername(username);
        return info;
    }
}

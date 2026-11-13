package com.zc.base.system.service;

import com.mybatisflex.core.query.QueryCondition;
import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.common.util.InMemoryRedisCache;
import com.zc.base.security.service.TokenRevokeService;
import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.cache.AuthUserInfo;
import com.zc.base.system.dto.ChangePasswordDTO;
import com.zc.base.system.dto.ProfileDTO;
import com.zc.base.system.dto.ProfileUpdateDTO;
import com.zc.base.system.entity.SysRole;
import com.zc.base.system.entity.SysUser;
import com.zc.base.system.entity.SysUserRole;
import com.zc.base.system.mapper.SysRoleMapper;
import com.zc.base.system.mapper.SysUserMapper;
import com.zc.base.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysProfileServiceTest {

    private static final Long USER_ID = 10L;
    private static final String OLD_PASSWORD = "oldpass123";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final AtomicInteger dbReads = new AtomicInteger();
    private SysUserMapper userMapper;
    private SysRoleMapper roleMapper;
    private SysUserRoleMapper userRoleMapper;
    private TokenRevokeService tokenRevokeService;
    private AuthUserCache authUserCache;
    private SysProfileService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        tokenRevokeService = mock(TokenRevokeService.class);
        authUserCache = new AuthUserCache(new InMemoryRedisCache(), new SecurityProperties());
        service = new SysProfileService(userMapper, roleMapper, userRoleMapper, passwordEncoder,
                authUserCache, tokenRevokeService);

        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setUsername("tester");
        user.setNickname("旧昵称");
        user.setEmail("old@example.com");
        user.setPhone("13800000000");
        user.setAvatar("old.png");
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode(OLD_PASSWORD));
        when(userMapper.selectOneById(USER_ID)).thenReturn(user);
        warmCache();
    }

    @Test
    void getReturnsProfileWithRoleNamesAndNoPassword() {
        when(userRoleMapper.selectListByCondition(any(QueryCondition.class)))
                .thenReturn(List.of(new SysUserRole(USER_ID, 1L)));
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleName("运营");
        when(roleMapper.selectListByCondition(any(QueryCondition.class))).thenReturn(List.of(role));

        ProfileDTO profile = service.get(USER_ID);

        assertEquals("tester", profile.getUsername());
        assertEquals("旧昵称", profile.getNickname());
        assertEquals(List.of("运营"), profile.getRoleNames());
    }

    @Test
    void updateOnlyWritesProfileFieldsAndEvictsCache() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setNickname("新昵称");
        dto.setEmail("new@example.com");
        dto.setPhone("13900000000");
        dto.setAvatar("new.png");

        service.update(USER_ID, dto);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).update(captor.capture());
        SysUser written = captor.getValue();
        assertEquals(USER_ID, written.getId());
        assertEquals("新昵称", written.getNickname());
        assertNull(written.getUsername(), "用户名不能通过个人中心修改");
        assertNull(written.getStatus(), "状态不能通过个人中心修改");
        assertNull(written.getPassword(), "不整行回写，避免覆盖并发修改的密码");
        assertTrue(cacheMissed(), "资料变更后缓存要失效，昵称才会刷新");
    }

    @Test
    void wrongOldPasswordIsRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changePassword(USER_ID, passwordDto("wrong-old1", "newpass123")));

        assertEquals("旧密码错误", e.getMessage());
        verify(userMapper, never()).update(any(SysUser.class));
        verify(tokenRevokeService, never()).revokeUser(any());
    }

    @Test
    void sameNewPasswordIsRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changePassword(USER_ID, passwordDto(OLD_PASSWORD, OLD_PASSWORD)));

        assertEquals("新密码不能与旧密码相同", e.getMessage());
        verify(userMapper, never()).update(any(SysUser.class));
        verify(tokenRevokeService, never()).revokeUser(any());
    }

    @Test
    void changePasswordStoresHashRevokesAllTokensAndEvictsCache() {
        service.changePassword(USER_ID, passwordDto(OLD_PASSWORD, "newpass123"));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).update(captor.capture());
        assertTrue(passwordEncoder.matches("newpass123", captor.getValue().getPassword()));
        assertNull(captor.getValue().getNickname(), "只写密码字段");
        verify(tokenRevokeService).revokeUser(USER_ID);
        assertTrue(cacheMissed());
    }

    private ChangePasswordDTO passwordDto(String oldPassword, String newPassword) {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(oldPassword);
        dto.setNewPassword(newPassword);
        return dto;
    }

    private void warmCache() {
        authUserCache.get("tester", () -> {
            dbReads.incrementAndGet();
            return Optional.of(new AuthUserInfo());
        });
        assertEquals(1, dbReads.get());
    }

    /** 缓存被清掉后，再读会重新查库 */
    private boolean cacheMissed() {
        authUserCache.get("tester", () -> {
            dbReads.incrementAndGet();
            return Optional.of(new AuthUserInfo());
        });
        return dbReads.get() == 2;
    }
}

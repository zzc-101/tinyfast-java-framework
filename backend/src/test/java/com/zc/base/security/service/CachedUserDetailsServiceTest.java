package com.zc.base.security.service;

import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.util.InMemoryRedisCache;
import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.entity.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachedUserDetailsServiceTest {

    private InMemoryRedisCache redis;
    private UserDetailsService db;
    private CachedUserDetailsService service;

    @BeforeEach
    void setUp() {
        redis = new InMemoryRedisCache();
        db = mock(UserDetailsService.class);
        service = new CachedUserDetailsService(new AuthUserCache(redis, new SecurityProperties()), db);

        SysUser user = new SysUser();
        user.setId(9L);
        user.setUsername("tester");
        user.setNickname("昵称");
        user.setPassword("$2a$10$hash");
        user.setStatus(1);
        SecurityUserDetails details = new SecurityUserDetails(user, Set.of("USER"), Set.of("system:user:list"));
        details.setRoleIds(List.of(3L));
        when(db.loadUserByUsername("tester")).thenReturn(details);
        when(db.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("ghost"));
    }

    @Test
    void secondLoadComesFromCacheWithSameFieldsAndNoPassword() {
        service.load("tester");
        SecurityUserDetails cached = service.load("tester").orElseThrow();

        verify(db, times(1)).loadUserByUsername("tester");
        assertEquals(9L, cached.getUserId());
        assertEquals("昵称", cached.getNickname());
        assertEquals(Set.of("USER"), cached.getRoles());
        assertEquals(Set.of("system:user:list"), cached.getPermissions());
        assertEquals(List.of(3L), cached.getRoleIds());
        assertTrue(cached.isEnabled());
        assertTrue(cached.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(cached.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("system:user:list")));
        assertNull(cached.getPassword());
    }

    @Test
    void passwordHashNeverReachesRedis() {
        service.load("tester");

        assertTrue(redis.<Object>hashGet("auth:user", "tester").orElseThrow().toString().indexOf("$2a$") < 0);
    }

    @Test
    void unknownUserIsEmptyAndQueriesDatabaseEveryTime() {
        assertTrue(service.load("ghost").isEmpty());
        assertTrue(service.load("ghost").isEmpty());
        verify(db, times(2)).loadUserByUsername("ghost");
    }
}

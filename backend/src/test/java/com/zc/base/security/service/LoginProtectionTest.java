package com.zc.base.security.service;

import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.common.util.InMemoryRedisCache;
import com.zc.base.security.dto.CaptchaResponse;
import com.zc.base.security.dto.LoginRequest;
import com.zc.base.security.event.LoginEvent;
import com.zc.base.security.util.JwtUtil;
import com.zc.base.system.entity.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录验证码和失败锁定。Redis 用内存版（走真实序列化器），密码校验用真实的 DaoAuthenticationProvider
 */
class LoginProtectionTest {

    private static final String USERNAME = "tester";
    private static final String PASSWORD = "abc12345";

    private InMemoryRedisCache redis;
    private SecurityProperties properties;
    private PasswordEncoder passwordEncoder;
    private UserDetailsService userDetailsService;
    private ApplicationEventPublisher eventPublisher;
    private CaptchaService captchaService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        redis = new InMemoryRedisCache();
        properties = new SecurityProperties();
        properties.getJwt().setSecret("test-secret-key-at-least-32-characters-long!!");
        properties.getCaptcha().setEnabled(false);

        passwordEncoder = new BCryptPasswordEncoder(4);
        userDetailsService = mock(UserDetailsService.class);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(user());
        when(userDetailsService.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("ghost"));
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        AuthenticationManager authenticationManager = new ProviderManager(provider);

        eventPublisher = mock(ApplicationEventPublisher.class);
        captchaService = new CaptchaService(redis, properties);
        authService = new AuthService(authenticationManager, new JwtUtil(properties), properties, redis,
                new TokenRevokeService(redis, properties), userDetailsService, eventPublisher,
                captchaService, new LoginAttemptService(redis, properties));
    }

    // ---------- 验证码 ----------

    @Test
    void captchaDisabledReturnsOnlyFlagAndLoginNeedsNoCode() {
        CaptchaResponse response = captchaService.generate();
        assertFalse(response.isEnabled());
        assertEquals(null, response.getCaptchaId());

        assertNotNull(authService.login(request(USERNAME, PASSWORD, null, null)));
    }

    @Test
    void generatedCaptchaCarriesIdAndImage() {
        properties.getCaptcha().setEnabled(true);
        CaptchaResponse response = captchaService.generate();
        assertTrue(response.isEnabled());
        assertTrue(response.getImage().startsWith("data:image/png;base64,"));
        assertTrue(redis.hasKey("captcha:" + response.getCaptchaId()));
    }

    @Test
    void missingCaptchaIsRejectedBeforePassword() {
        properties.getCaptcha().setEnabled(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> authService.login(request(USERNAME, PASSWORD, null, null)));

        assertEquals(400, e.getCode());
        assertEquals("请输入验证码", e.getMessage());
    }

    @Test
    void wrongCaptchaIsRejectedAndDoesNotCountAsPasswordFailure() {
        properties.getCaptcha().setEnabled(true);
        CaptchaResponse captcha = captchaService.generate();

        BusinessException e = assertThrows(BusinessException.class,
                () -> authService.login(request(USERNAME, PASSWORD, captcha.getCaptchaId(), "wrong")));

        assertEquals("验证码错误", e.getMessage());
        assertFalse(redis.hasKey("login:fail:" + USERNAME));
        assertEquals("验证码错误", lastEvent().message());
    }

    @Test
    void wrongAttemptConsumesTheCaptcha() {
        properties.getCaptcha().setEnabled(true);
        CaptchaResponse captcha = captchaService.generate();
        String answer = answer(captcha);

        assertThrows(BusinessException.class, () -> captchaService.verify(captcha.getCaptchaId(), "wrong"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> captchaService.verify(captcha.getCaptchaId(), answer));
        assertEquals("验证码已过期，请刷新后重试", e.getMessage());
    }

    @Test
    void correctCaptchaWorksOnceAndIsCaseInsensitive() {
        properties.getCaptcha().setEnabled(true);
        CaptchaResponse captcha = captchaService.generate();
        String answer = answer(captcha);

        assertNotNull(authService.login(request(USERNAME, PASSWORD, captcha.getCaptchaId(), answer.toUpperCase())));

        BusinessException e = assertThrows(BusinessException.class,
                () -> authService.login(request(USERNAME, PASSWORD, captcha.getCaptchaId(), answer)));
        assertEquals(400, e.getCode(), "用过一次就失效");
    }

    @Test
    void expiredCaptchaIsRejected() {
        properties.getCaptcha().setEnabled(true);
        CaptchaResponse captcha = captchaService.generate();
        String answer = answer(captcha);

        redis.advance(2 * 60 * 1000L + 1000);

        BusinessException e = assertThrows(BusinessException.class,
                () -> captchaService.verify(captcha.getCaptchaId(), answer));
        assertEquals("验证码已过期，请刷新后重试", e.getMessage());
    }

    // ---------- 失败锁定 ----------

    @Test
    void locksAfterMaxFailuresAndRejectsEvenCorrectPassword() {
        for (int i = 1; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(request(USERNAME, "wrong-1", null, null)));
        }

        BusinessException locked = assertThrows(BusinessException.class,
                () -> authService.login(request(USERNAME, "wrong-1", null, null)));
        assertEquals(429, locked.getCode());
        assertEquals("密码错误次数过多，账号已锁定 15 分钟", locked.getMessage());
        assertEquals(locked.getMessage(), lastEvent().message(), "锁定也写登录日志");

        // 锁定期间密码正确也拒绝，且不会去校验密码
        BusinessException rejected = assertThrows(BusinessException.class,
                () -> authService.login(request(USERNAME, PASSWORD, null, null)));
        assertEquals(429, rejected.getCode());
        assertEquals("账号已锁定，请 15 分钟后再试", rejected.getMessage());
        assertFalse(lastEvent().success());
        verify(userDetailsService, times(5)).loadUserByUsername(USERNAME);

        redis.advance(10 * 60 * 1000L);
        assertEquals("账号已锁定，请 5 分钟后再试", assertThrows(BusinessException.class,
                () -> authService.login(request(USERNAME, PASSWORD, null, null))).getMessage());
    }

    @Test
    void lockExpiresAfterLockMinutes() {
        lock(USERNAME);

        redis.advance(15 * 60 * 1000L + 1000);

        assertNotNull(authService.login(request(USERNAME, PASSWORD, null, null)));
    }

    @Test
    void successfulLoginResetsTheCounter() {
        for (int i = 0; i < 4; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(request(USERNAME, "wrong-1", null, null)));
        }
        assertNotNull(authService.login(request(USERNAME, PASSWORD, null, null)));

        // 清零后再错 4 次仍然不锁定
        for (int i = 0; i < 4; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(request(USERNAME, "wrong-1", null, null)));
        }
        assertNotNull(authService.login(request(USERNAME, PASSWORD, null, null)));
    }

    @Test
    void unknownUsernameIsCountedAndLooksTheSameAsWrongPassword() {
        BadCredentialsException unknown = assertThrows(BadCredentialsException.class,
                () -> authService.login(request("ghost", "whatever1", null, null)));
        BadCredentialsException wrong = assertThrows(BadCredentialsException.class,
                () -> authService.login(request(USERNAME, "wrong-1", null, null)));
        assertEquals(wrong.getMessage(), unknown.getMessage());

        for (int i = 0; i < 3; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(request("ghost", "whatever1", null, null)));
        }
        BusinessException locked = assertThrows(BusinessException.class,
                () -> authService.login(request("ghost", "whatever1", null, null)));
        assertEquals("密码错误次数过多，账号已锁定 15 分钟", locked.getMessage(), "不存在的用户名同样会被锁定，提示与真实用户一致");
    }

    @Test
    void lockIgnoresCaseAndTrailingSpaces() {
        lock(USERNAME);

        assertThrows(BusinessException.class, () -> authService.login(request(" TESTER ", PASSWORD, null, null)));
    }

    @Test
    void failureEventsAreRecordedForEachWrongPassword() {
        assertThrows(BadCredentialsException.class, () -> authService.login(request(USERNAME, "wrong-1", null, null)));

        ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertEquals("用户名或密码错误", captor.getValue().message());
    }

    private void lock(String username) {
        for (int i = 0; i < 5; i++) {
            try {
                authService.login(request(username, "wrong-1", null, null));
            } catch (RuntimeException ignored) {
                // 前 4 次是 BadCredentialsException，第 5 次是锁定
            }
        }
    }

    private String answer(CaptchaResponse captcha) {
        return redis.<String>get("captcha:" + captcha.getCaptchaId()).orElseThrow();
    }

    private LoginEvent lastEvent() {
        ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        List<LoginEvent> events = captor.getAllValues();
        return events.get(events.size() - 1);
    }

    private LoginRequest request(String username, String password, String captchaId, String captchaCode) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setCaptchaId(captchaId);
        request.setCaptchaCode(captchaCode);
        return request;
    }

    private SecurityUserDetails user() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername(USERNAME);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setStatus(1);
        return new SecurityUserDetails(user, Set.of("USER"), Set.of());
    }
}

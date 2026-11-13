package com.zc.base.security.service;

import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.common.util.RedisCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败计数和锁定，都存在 Redis。
 * 失败计数在最后一次失败后保留 lockMinutes 分钟；达到上限后写入锁定 key，锁定期间不再校验密码。
 * 用户名去空格并转小写后作 key：数据库用户名不区分大小写，且忽略末尾空格。
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String FAIL_PREFIX = "login:fail:";
    private static final String LOCK_PREFIX = "login:lock:";

    private final RedisCache redisCache;
    private final SecurityProperties securityProperties;

    /**
     * 账号处于锁定期时抛 BusinessException(429)，提示剩余等待时间
     */
    public void checkLocked(String username) {
        long seconds = redisCache.getExpire(LOCK_PREFIX + normalize(username));
        if (seconds > 0) {
            throw new BusinessException(429, "账号已锁定，请 " + toMinutes(seconds) + " 分钟后再试");
        }
    }

    /**
     * 记录一次密码错误（用户名不存在也要记）。
     *
     * @return 本次失败后是否触发了锁定
     */
    public boolean recordFailure(String username) {
        SecurityProperties.Login config = securityProperties.getLogin();
        String name = normalize(username);
        String failKey = FAIL_PREFIX + name;

        long count = redisCache.increment(failKey);
        redisCache.expire(failKey, config.getLockMinutes(), TimeUnit.MINUTES);
        if (count < config.getMaxFailCount()) {
            return false;
        }
        redisCache.set(LOCK_PREFIX + name, "1", config.getLockMinutes(), TimeUnit.MINUTES);
        redisCache.delete(failKey);
        return true;
    }

    /**
     * 登录成功，清零失败计数
     */
    public void clearFailures(String username) {
        redisCache.delete(FAIL_PREFIX + normalize(username));
    }

    /**
     * 触发锁定时给用户看的提示
     */
    public String lockedMessage() {
        return "密码错误次数过多，账号已锁定 " + securityProperties.getLogin().getLockMinutes() + " 分钟";
    }

    private String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private long toMinutes(long seconds) {
        return (seconds + 59) / 60;
    }

}

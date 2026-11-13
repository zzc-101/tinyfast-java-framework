package com.zc.base.system.cache;

import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.util.RedisCache;
import com.zc.base.common.util.TransactionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 鉴权用户信息缓存，JwtAuthenticationFilter 每次请求读这里，不再查库。
 * <p>
 * 所有用户放在同一个 Redis Hash（field 是用户名），"全部失效"直接删这个 key，不需要 KEYS 扫描；
 * 读一个用户只要一次 HGET。Hash 不支持单个 field 过期，所以每条记录自带写入时间，超过缓存时间按未命中处理。
 * <p>
 * 失效方法在事务提交后才执行（没有事务则立即执行），避免事务未提交时别的请求把旧数据读回缓存。
 * 另有一个失效计数器：读库前后计数器变了，说明读库期间发生过失效，这次读到的数据不写缓存。
 * 剩余的窗口只有"校验计数器到写入缓存"之间的一次 Redis 往返。
 * 失效失败（如 Redis 不可用）只记错误日志，不向上抛：此时数据库事务已经提交，抛异常会让调用方误以为操作失败。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUserCache {

    static final String USERS_KEY = "auth:user";
    static final String EPOCH_KEY = "auth:user:epoch";

    private final RedisCache redisCache;
    private final SecurityProperties securityProperties;

    /**
     * 读缓存，未命中时用 loader 查库并写回。Redis 读失败时直接退回 loader。
     */
    public Optional<AuthUserInfo> get(String username, Supplier<Optional<AuthUserInfo>> loader) {
        long epoch;
        try {
            Optional<AuthUserInfo> cached = redisCache.<Object>hashGet(USERS_KEY, username)
                    .filter(AuthUserInfo.class::isInstance)
                    .map(AuthUserInfo.class::cast)
                    .filter(info -> System.currentTimeMillis() - info.getCachedAt() < ttlMillis());
            if (cached.isPresent()) {
                return cached;
            }
            epoch = currentEpoch();
        } catch (RuntimeException e) {
            log.warn("读取鉴权用户缓存失败，退回查库: {}", e.getMessage());
            return loader.get();
        }

        Optional<AuthUserInfo> loaded = loader.get();
        loaded.ifPresent(info -> put(username, info, epoch));
        return loaded;
    }

    /**
     * 使指定用户名的缓存失效。用户改名时旧名和新名都要传。
     */
    public void evictUser(String... usernames) {
        TransactionUtil.afterCommit(() -> {
            try {
                redisCache.increment(EPOCH_KEY);
                for (String username : usernames) {
                    redisCache.hashDelete(USERS_KEY, username);
                }
            } catch (RuntimeException e) {
                log.error("鉴权用户缓存失效失败，最长 {} 分钟后才会自然过期: {}", securityProperties.getUserCacheMinutes(), usernames, e);
            }
        });
    }

    /**
     * 使全部用户的缓存失效。角色、菜单变更时使用。
     */
    public void evictAll() {
        TransactionUtil.afterCommit(() -> {
            try {
                redisCache.increment(EPOCH_KEY);
                redisCache.delete(USERS_KEY);
            } catch (RuntimeException e) {
                log.error("鉴权用户缓存全部失效失败，最长 {} 分钟后才会自然过期", securityProperties.getUserCacheMinutes(), e);
            }
        });
    }

    private void put(String username, AuthUserInfo info, long epochBeforeLoad) {
        try {
            if (currentEpoch() != epochBeforeLoad) {
                return;
            }
            info.setCachedAt(System.currentTimeMillis());
            redisCache.hashPut(USERS_KEY, username, info);
            redisCache.expire(USERS_KEY, securityProperties.getUserCacheMinutes(), TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            log.warn("写入鉴权用户缓存失败: {}", e.getMessage());
        }
    }

    private long currentEpoch() {
        return redisCache.<Number>get(EPOCH_KEY).map(Number::longValue).orElse(0L);
    }

    private long ttlMillis() {
        return TimeUnit.MINUTES.toMillis(securityProperties.getUserCacheMinutes());
    }

}

package com.zc.base.system.cache;

import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.util.InMemoryRedisCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AuthUserCacheTest {

    private InMemoryRedisCache redis;
    private SecurityProperties properties;
    private AuthUserCache cache;
    private final AtomicInteger dbReads = new AtomicInteger();

    @BeforeEach
    void setUp() {
        redis = new InMemoryRedisCache();
        properties = new SecurityProperties();
        cache = new AuthUserCache(redis, properties);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void realSerializerRoundTripKeepsAllFields() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        AuthUserInfo info = info(7L, "tester");

        Object read = serializer.deserialize(serializer.serialize(info));

        AuthUserInfo restored = assertInstanceOf(AuthUserInfo.class, read);
        assertEquals(info, restored);
        assertEquals(Set.of("system:user:list", "system:user:add"), restored.getPermissions());
        assertEquals(Long.valueOf(7L), restored.getUserId());
        assertEquals(Long.valueOf(3L), restored.getRoleIds().get(0));
    }

    @Test
    void epochWrittenByIncrementIsReadableAsNumber() {
        // INCR 写入的是纯数字文本，要能被 RedisCache.get 用的反序列化器读成数字
        assertEquals(5, new GenericJackson2JsonRedisSerializer().deserialize("5".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void hitDoesNotQueryDatabase() {
        get("tester");
        AuthUserInfo hit = get("tester").orElseThrow();

        assertEquals(1, dbReads.get());
        // 命中的是写进 Redis 再读出来的对象，各字段和原始数据一致
        AuthUserInfo expected = info(7L, "tester");
        assertEquals(expected.getUserId(), hit.getUserId());
        assertEquals(expected.getRoles(), hit.getRoles());
        assertEquals(expected.getPermissions(), hit.getPermissions());
        assertEquals(expected.getRoleIds(), hit.getRoleIds());
    }

    @Test
    void missingUserIsNotCached() {
        cache.get("ghost", () -> {
            dbReads.incrementAndGet();
            return Optional.empty();
        });
        cache.get("ghost", () -> {
            dbReads.incrementAndGet();
            return Optional.empty();
        });
        assertEquals(2, dbReads.get());
    }

    @Test
    void evictUserOnlyAffectsThatUser() {
        get("a");
        get("b");

        cache.evictUser("a");
        get("a");
        get("b");

        assertEquals(3, dbReads.get());
    }

    @Test
    void evictUserWithOldAndNewUsername() {
        get("old");
        get("new");

        cache.evictUser("old", "new");
        get("old");
        get("new");

        assertEquals(4, dbReads.get());
    }

    @Test
    void evictAllClearsEveryUser() {
        get("a");
        get("b");

        cache.evictAll();
        get("a");
        get("b");

        assertEquals(4, dbReads.get());
    }

    @Test
    void entryOlderThanCacheTimeIsReloaded() {
        properties.setUserCacheMinutes(0);
        get("a");
        get("a");
        assertEquals(2, dbReads.get());
    }

    @Test
    void redisReadFailureFallsBackToDatabase() {
        redis.failReads = true;

        Optional<AuthUserInfo> result = get("a");
        get("a");

        assertEquals("a", result.orElseThrow().getUsername());
        assertEquals(2, dbReads.get());
    }

    @Test
    void evictionDuringDatabaseReadPreventsStaleWriteBack() {
        // 读库期间发生了失效，这次读到的（可能是旧的）数据不能写回缓存
        cache.get("a", () -> {
            dbReads.incrementAndGet();
            cache.evictAll();
            return Optional.of(info(1L, "a"));
        });
        get("a");

        assertEquals(2, dbReads.get());
    }

    @Test
    void evictionInsideTransactionWaitsForCommit() {
        get("a");
        TransactionSynchronizationManager.initSynchronization();

        cache.evictUser("a");
        cache.evictAll();
        get("a");
        assertEquals(1, dbReads.get(), "事务未提交，缓存不能被清掉");

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        TransactionSynchronizationManager.clear();
        get("a");
        assertEquals(2, dbReads.get());
    }

    @Test
    void evictionInsideRolledBackTransactionDoesNothing() {
        get("a");
        TransactionSynchronizationManager.initSynchronization();

        cache.evictUser("a");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        TransactionSynchronizationManager.clear();
        get("a");

        assertEquals(1, dbReads.get());
    }

    private Optional<AuthUserInfo> get(String username) {
        return cache.get(username, () -> {
            dbReads.incrementAndGet();
            return Optional.of(info(7L, username));
        });
    }

    private AuthUserInfo info(Long userId, String username) {
        AuthUserInfo info = new AuthUserInfo();
        info.setUserId(userId);
        info.setUsername(username);
        info.setNickname("昵称");
        info.setStatus(1);
        info.setRoles(new LinkedHashSet<>(Set.of("USER")));
        info.setPermissions(new LinkedHashSet<>(Set.of("system:user:list", "system:user:add")));
        info.setRoleIds(new ArrayList<>(List.of(3L, 4L)));
        return info;
    }
}

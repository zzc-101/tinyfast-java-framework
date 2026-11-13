package com.zc.base.common.util;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 测试用的内存版 RedisCache。
 * 值要经过项目 RedisConfig 里用的同一个 GenericJackson2JsonRedisSerializer 序列化再反序列化，
 * 所以写进去的对象能不能读回来，和真实 Redis 一致。支持过期（用 advance 拨快时间）和模拟读失败。
 */
public class InMemoryRedisCache extends RedisCache {

    private final RedisSerializer<Object> serializer = new GenericJackson2JsonRedisSerializer();
    private final Map<String, byte[]> values = new HashMap<>();
    private final Map<String, Map<String, byte[]>> hashes = new HashMap<>();
    private final Map<String, Long> expireAt = new HashMap<>();
    private long now = 1_000_000L;

    /** 为 true 时所有读操作抛异常，模拟 Redis 不可用 */
    public boolean failReads;

    public InMemoryRedisCache() {
        super(null);
    }

    public void advance(long millis) {
        now += millis;
    }

    @Override
    public void set(String key, Object value) {
        evictIfExpired(key);
        values.put(key, serializer.serialize(value));
        expireAt.remove(key);
    }

    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        set(key, value);
        expire(key, timeout, unit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        checkRead();
        evictIfExpired(key);
        return Optional.ofNullable(values.get(key)).map(bytes -> (T) serializer.deserialize(bytes));
    }

    @Override
    public boolean delete(String key) {
        evictIfExpired(key);
        boolean existed = values.remove(key) != null | hashes.remove(key) != null;
        expireAt.remove(key);
        return existed;
    }

    @Override
    public void delete(Collection<String> keys) {
        keys.forEach(this::delete);
    }

    @Override
    public boolean hasKey(String key) {
        checkRead();
        evictIfExpired(key);
        return values.containsKey(key) || hashes.containsKey(key);
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        evictIfExpired(key);
        if (!values.containsKey(key) && !hashes.containsKey(key)) {
            return false;
        }
        expireAt.put(key, now + unit.toMillis(timeout));
        return true;
    }

    @Override
    public long getExpire(String key) {
        evictIfExpired(key);
        if (!values.containsKey(key) && !hashes.containsKey(key)) {
            return -2;
        }
        Long at = expireAt.get(key);
        return at == null ? -1 : (at - now) / 1000;
    }

    @Override
    public long increment(String key) {
        evictIfExpired(key);
        // 和真实 Redis 一样，INCR 写入的是不带序列化的纯数字文本
        long next = Optional.ofNullable(values.get(key))
                .map(bytes -> Long.parseLong(new String(bytes, StandardCharsets.UTF_8)))
                .orElse(0L) + 1;
        values.put(key, String.valueOf(next).getBytes(StandardCharsets.UTF_8));
        return next;
    }

    @Override
    public void hashPut(String key, String field, Object value) {
        evictIfExpired(key);
        hashes.computeIfAbsent(key, k -> new HashMap<>()).put(field, serializer.serialize(value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> hashGet(String key, String field) {
        checkRead();
        evictIfExpired(key);
        return Optional.ofNullable(hashes.get(key))
                .map(hash -> hash.get(field))
                .map(bytes -> (T) serializer.deserialize(bytes));
    }

    @Override
    public void hashDelete(String key, String field) {
        evictIfExpired(key);
        Optional.ofNullable(hashes.get(key)).ifPresent(hash -> hash.remove(field));
    }

    private void checkRead() {
        if (failReads) {
            throw new IllegalStateException("Redis 不可用（测试模拟）");
        }
    }

    private void evictIfExpired(String key) {
        Long at = expireAt.get(key);
        if (at != null && at <= now) {
            values.remove(key);
            hashes.remove(key);
            expireAt.remove(key);
        }
    }

}

package com.zc.base.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存工具类
 */
@Component
@RequiredArgsConstructor
public class RedisCache {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable((T) value);
    }

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 批量删除
     */
    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 判断key是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /**
     * 获取过期时间
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key);
        return Optional.ofNullable(expire).orElse(-1L);
    }

    /**
     * 自增（key 不存在时从 0 开始），返回自增后的值
     */
    public long increment(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().increment(key)).orElse(0L);
    }

    /**
     * 设置 Hash 字段
     */
    public void hashPut(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 获取 Hash 字段
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> hashGet(String key, String field) {
        return Optional.ofNullable((T) redisTemplate.opsForHash().get(key, field));
    }

    /**
     * 删除 Hash 字段
     */
    public void hashDelete(String key, String field) {
        redisTemplate.opsForHash().delete(key, field);
    }

    /**
     * 模糊匹配keys
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

}

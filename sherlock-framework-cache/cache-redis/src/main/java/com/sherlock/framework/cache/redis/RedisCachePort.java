package com.sherlock.framework.cache.redis;

import com.sherlock.framework.cache.CachePort;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class RedisCachePort implements CachePort {

    private final StringRedisTemplate redisTemplate;
    private final TenantCacheKeyFactory keyFactory;

    public RedisCachePort(StringRedisTemplate redisTemplate, TenantCacheKeyFactory keyFactory) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory must not be null");
    }

    @Override
    public Optional<String> get(String namespace, String resource) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(namespace, resource)));
    }

    @Override
    public void put(String namespace, String resource, String value, Duration ttl) {
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        redisTemplate.opsForValue().set(key(namespace, resource), value, ttl);
    }

    @Override
    public void evict(String namespace, String resource) {
        redisTemplate.delete(key(namespace, resource));
    }

    private String key(String namespace, String resource) {
        return keyFactory.create(namespace, resource).value();
    }
}

package com.sherlock.framework.cache.redis;

import com.sherlock.framework.tenant.TenantContext;
import com.sherlock.framework.tenant.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisCachePortIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4.2-alpine")
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;

    @AfterEach
    void clearTenantAndConnection() {
        TenantContext.clear();
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void sameResourceIsIsolatedPerTenant() {
        RedisCachePort cache = cache();

        TenantContext.set(new TenantId(1L));
        cache.put("order", "same-resource", "tenant-one", Duration.ofMinutes(1));
        assertThat(cache.get("order", "same-resource")).contains("tenant-one");

        TenantContext.set(new TenantId(2L));
        assertThat(cache.get("order", "same-resource")).isEmpty();
        cache.put("order", "same-resource", "tenant-two", Duration.ofMinutes(1));
        assertThat(cache.get("order", "same-resource")).contains("tenant-two");

        TenantContext.set(new TenantId(1L));
        assertThat(cache.get("order", "same-resource")).contains("tenant-one");
    }

    @Test
    void putRejectsNonPositiveTtl() {
        RedisCachePort cache = cache();
        TenantContext.set(new TenantId(1L));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> cache.put("order", "1001", "value", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RedisCachePort cache() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getFirstMappedPort());
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        return new RedisCachePort(redisTemplate, new TenantCacheKeyFactory());
    }
}

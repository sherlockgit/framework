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
    void roundTripsStringValues() {
        RedisCachePort cache = cache();
        TenantContext.set(new TenantId(1L));

        cache.put("order", "1001", "cached-value", Duration.ofMinutes(1));

        assertThat(cache.get("order", "1001")).contains("cached-value");
    }

    @Test
    void expiresEntriesAfterTheirTtl() throws InterruptedException {
        RedisCachePort cache = cache();
        TenantContext.set(new TenantId(1L));

        cache.put("order", "1001", "cached-value", Duration.ofMillis(100));

        assertThat(cache.get("order", "1001")).contains("cached-value");
        awaitMissing(cache, "order", "1001");
    }

    @Test
    void evictsAnExistingEntry() {
        RedisCachePort cache = cache();
        TenantContext.set(new TenantId(1L));
        cache.put("order", "1001", "cached-value", Duration.ofMinutes(1));

        cache.evict("order", "1001");

        assertThat(cache.get("order", "1001")).isEmpty();
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

    private void awaitMissing(RedisCachePort cache, String namespace, String resource) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < deadline) {
            if (cache.get(namespace, resource).isEmpty()) {
                return;
            }
            Thread.sleep(25);
        }
        assertThat(cache.get(namespace, resource)).isEmpty();
    }
}

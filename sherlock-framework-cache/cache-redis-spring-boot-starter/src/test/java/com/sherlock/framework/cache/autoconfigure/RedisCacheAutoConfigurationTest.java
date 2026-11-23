package com.sherlock.framework.cache.autoconfigure;

import com.sherlock.framework.cache.CachePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisCacheAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisCacheAutoConfiguration.class));

    @Test
    void userProvidedCachePortPreventsAutoConfiguration() {
        contextRunner
                .withUserConfiguration(UserCachePortConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(CachePort.class)
                        .hasBean("userCachePort"));
    }

    @Configuration(proxyBeanMethods = false)
    static class UserCachePortConfiguration {

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return new StringRedisTemplate(mock(RedisConnectionFactory.class));
        }

        @Bean
        CachePort userCachePort() {
            return new CachePort() {
                @Override
                public Optional<String> get(String namespace, String resource) {
                    return Optional.empty();
                }

                @Override
                public void put(String namespace, String resource, String value, Duration ttl) {
                }

                @Override
                public void evict(String namespace, String resource) {
                }
            };
        }
    }
}

package com.sherlock.framework.cache.autoconfigure;

import com.sherlock.framework.cache.CachePort;
import com.sherlock.framework.cache.redis.RedisCachePort;
import com.sherlock.framework.cache.redis.TenantCacheKeyFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TenantCacheKeyFactory tenantCacheKeyFactory() {
        return new TenantCacheKeyFactory();
    }

    @Bean
    @ConditionalOnMissingBean(CachePort.class)
    CachePort redisCachePort(StringRedisTemplate redisTemplate, TenantCacheKeyFactory keyFactory) {
        return new RedisCachePort(redisTemplate, keyFactory);
    }
}

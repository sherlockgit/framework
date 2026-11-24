# cache-redis-spring-boot-starter 类说明

## `com.sherlock.framework.cache.autoconfigure`

### `RedisCacheAutoConfiguration`

位于 Redis 自动配置之后的条件自动装配类。存在 `StringRedisTemplate` 时创建键工厂；缺少用户自定义 `CachePort` 时创建 `RedisCachePort`。

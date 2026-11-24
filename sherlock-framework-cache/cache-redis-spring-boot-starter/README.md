# cache-redis-spring-boot-starter

## 职责

引入 `spring-boot-starter-data-redis`，在存在 `StringRedisTemplate` 时自动注册 `TenantCacheKeyFactory` 和 `RedisCachePort`。

## 覆盖规则

用户提供 `CachePort` 时默认实现不会覆盖。该模块只负责 Redis 集成，不负责缓存失效策略或分布式锁。

生产类设计见 [docs/classes.md](docs/classes.md)。

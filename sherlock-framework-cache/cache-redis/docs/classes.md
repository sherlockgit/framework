# cache-redis 类说明

## `com.sherlock.framework.cache.redis`

### `TenantCacheKeyFactory`

根据当前 `TenantContext` 将业务命名空间和资源生成 `CacheKey`。缺少租户时立即失败，不允许创建全局默认缓存键。

### `RedisCachePort`

`CachePort` 的 Redis 实现。使用 `StringRedisTemplate` 读写 UTF-8 字符串，写入要求正 TTL；所有键均由 `TenantCacheKeyFactory` 构造。

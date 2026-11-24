# cache-redis

使用 `StringRedisTemplate` 实现 `CachePort`。所有读写先通过 `TenantCacheKeyFactory` 调用 `TenantContext.require()` 生成租户键；Redis 操作失败由调用方按业务需要处理，不在适配器中静默吞掉异常。

真实 Redis 读写、TTL、驱逐和租户隔离由 Testcontainers 验证，需要 Docker。

生产类设计见 [docs/classes.md](docs/classes.md)。

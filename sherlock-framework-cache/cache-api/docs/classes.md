# cache-api 类说明

## `com.sherlock.framework.cache`

### `CacheKey`

结构化缓存键，包含命名空间、租户 ID 和资源。每个分段禁止为空、冒号或空白，保证 `namespace:tenantId:resource` 格式无歧义。

### `CachePort`

缓存端口，只暴露 `get`、`put`、`evict` 的命名空间和资源参数，不接受原始 Redis key。使用字符串值与 `Duration` TTL，避免调用方绕过租户命名空间。

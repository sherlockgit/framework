# cache-api

定义不暴露原始 Redis key 的缓存契约。所有框架缓存操作必须提供业务命名空间和资源标识，由实现生成包含当前租户的结构化键。

缓存值使用 UTF-8 字符串，不使用 Java 对象序列化；写入 TTL 必须为正值。

生产类设计见 [docs/classes.md](docs/classes.md)。

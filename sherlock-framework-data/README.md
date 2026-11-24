# sherlock-framework-data

聚合数据范围、MyBatis 共享表隔离与数据层自动装配模块。

| 叶子模块 | 职责 |
| --- | --- |
| [`data-scope-api`](data-scope-api/README.md) | 数据范围模型、解析与线程上下文。 |
| [`data-mybatis`](data-mybatis/README.md) | 租户 SQL 条件、数据范围 SQL 条件及写入保护。 |
| [`data-spring-boot-starter`](data-spring-boot-starter/README.md) | MyBatis 插件、表映射校验与上下文清理自动装配。 |

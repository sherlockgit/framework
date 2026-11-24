# Sherlock Framework

Sherlock Framework 是面向多租户 SaaS 的 Java 微服务基础框架。它以 Java 21、Spring Boot 3、MySQL 8、Redis 和 RabbitMQ 为技术基线，提供可按需引入的 Maven 模块与 Spring Boot Starter。

## 架构原则

- `base`、租户、认证、数据范围和事件契约保持纯 Java，不依赖 Spring 或中间件客户端。
- Starter 只负责自动装配与运行环境集成；业务服务依赖 Starter，不直接依赖框架已封装的中间件客户端 API。
- 多租户采用共享库、共享表模式。租户表默认必须包含 `tenant_id`，数据层同时执行租户条件和数据范围条件。
- 所有请求在服务内独立验证 JWT；网关只作为纵深防御，不是唯一安全边界。

## 模块地图

| 模块 | 职责 |
| --- | --- |
| [`sherlock-framework-bom`](sherlock-framework-bom/README.md) | 锁定经过验证的第三方依赖版本。 |
| [`sherlock-framework-base`](sherlock-framework-base/README.md) | 错误码、异常、统一响应和分页模型。 |
| [`sherlock-framework-tenant`](sherlock-framework-tenant/README.md) | 租户标识、上下文和 Servlet 清理。 |
| [`sherlock-framework-security`](sherlock-framework-security/README.md) | JWT、认证主体、权限映射与 Spring Security 集成。 |
| [`sherlock-framework-web`](sherlock-framework-web/README.md) | `traceId`、统一异常响应和 Web 自动装配。 |
| [`sherlock-framework-data`](sherlock-framework-data/README.md) | 数据范围、MyBatis 租户隔离及数据层自动装配。 |
| [`sherlock-framework-cache`](sherlock-framework-cache/README.md) | Redis 租户缓存键与缓存抽象。 |
| [`sherlock-framework-platform`](sherlock-framework-platform/README.md) | 后续平台 Starter 的聚合入口。 |
| [`sherlock-platform-services`](sherlock-platform-services/README.md) | 后续网关、IAM、租户和审计服务的聚合入口。 |
| [`examples/tenant-demo`](examples/tenant-demo/README.md) | 多租户订单示例与端到端验收夹具。 |

各叶子模块的职责、配置和接入方式见其 `README.md`；生产类型的类级设计说明见模块内 `docs/classes.md`。

## 技术基线

| 领域 | 选型 |
| --- | --- |
| 运行时 | Java 21 |
| 构建 | Maven Wrapper 3.9.9 |
| 应用框架 | Spring Boot 3.4.x |
| 持久化 | MySQL 8、MyBatis-Plus、Flyway |
| 安全 | Spring Security、RS256 JWT |
| 缓存 | Redis、Lettuce |
| 测试 | JUnit 5、AssertJ、Testcontainers |

## 构建与验证

需要 JDK 21。使用 Maven Wrapper 执行完整验证：

```powershell
.\mvnw.cmd clean verify
```

MySQL、Redis 和示例端到端测试使用 Testcontainers，需要可用的 Docker 环境。没有 Docker 时，相关测试会被明确跳过，不能替代真实容器验收。

## 文档与提交规范

- 项目说明、模块说明、类级设计说明和 Java 注释使用中文；代码标识符保持原样。
- 生产类新增、删除或行为变更时，同步维护所在模块的 `docs/classes.md`。
- 模块职责、配置、接入方式或测试策略变更时，同步维护模块 `README.md`。
- 本项目的 Git 提交信息使用中文。

详细规范由项目内 [维护 Java 框架文档 Skill](.codex/skills/maintaining-java-framework-docs/SKILL.md) 定义。

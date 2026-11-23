# Sherlock Framework 平台设计

## 状态

第一期实施方案，已确认。

## 目标

建设一套基于 Java 21 和 Spring Boot 3 的微服务平台，用于运行在传统虚拟机或物理机环境中的多租户 SaaS 应用。平台同时提供可复用的应用公共依赖包，以及少量可独立部署的控制面服务。

整体交付范围是完整云平台套件，内部组织方式采用“契约核心 + 基础设施适配器”：稳定的纯 Java 契约优先，Spring Boot Starter 负责集成，业务服务不直接绑定具体中间件客户端。

## 已确定的技术基线

| 领域 | 决策 |
| --- | --- |
| 语言与运行时 | Java 21 |
| 应用框架 | Spring Boot 3.x |
| 构建与发布 | Maven 多模块工程 |
| 关系型数据库 | MySQL 8 |
| 数据库迁移 | Flyway |
| 租户存储 | 共享库、共享表，以 `tenant_id` 隔离 |
| 认证 | 内置账号密码登录和 JWT |
| 缓存 | 仅 Redis |
| 消息 | RabbitMQ |
| 服务注册与配置 | Nacos |
| 外部 API 入口 | Spring Cloud Gateway |
| 指标 | Prometheus 和 Grafana |
| 日志 | Loki |
| 链路追踪 | OpenTelemetry 和 Grafana Tempo |
| 部署目标 | 虚拟机或物理机 |

## 总体架构

平台由四层组成，依赖方向只能单向向下：

```text
业务服务、网关、IAM、租户服务、审计服务
    -> Spring Boot Starter
    -> 纯 Java 契约
    -> 基础设施适配器与外部平台
```

`base`、租户、认证、数据范围、事件和可观测性契约不得依赖 Spring、MyBatis、Redis、RabbitMQ、Nacos 或任何可观测性厂商。Starter 负责自动装配与配置属性，基础设施适配器承载厂商相关实现。

业务服务通过 BOM 锁定版本，只按需引入 Starter。对于框架已覆盖的能力，业务代码不得直接依赖 Nacos、RabbitMQ 或 Redis 客户端 API。

### 可部署的平台服务

| 服务 | 职责 |
| --- | --- |
| `api-gateway` | 外部统一入口、JWT 前置校验、租户感知限流、路由及请求关联。 |
| `iam-service` | 账号密码登录、刷新令牌生命周期、JWT 签发和吊销、租户内角色权限、部门及数据范围管理。 |
| `tenant-service` | 租户生命周期、租户级配置、配额和租户元数据。 |
| `audit-service` | 异步消费并持久化审计操作，支持合规查询和导出。 |

网关校验仅是纵深防御。每个业务服务都必须独立校验 JWT，并自行建立认证与租户上下文。登录完成后，IAM 不处于同步业务请求链路；服务使用本地验签的令牌及租户隔离的策略快照缓存。

## Maven 模块

```text
sherlock-framework-bom
sherlock-framework-base

sherlock-framework-tenant
  tenant-api
  tenant-spring-boot-starter

sherlock-framework-web
  web-core
  web-spring-boot-starter

sherlock-framework-security
  security-api
  security-jwt
  security-spring-boot-starter

sherlock-framework-data
  data-scope-api
  data-mybatis
  data-spring-boot-starter

sherlock-framework-cache
  cache-api
  cache-redis
  cache-redis-spring-boot-starter

sherlock-framework-mq
  event-api
  mq-rabbitmq
  mq-rabbitmq-spring-boot-starter

sherlock-framework-platform
  nacos-spring-boot-starter
  observability-spring-boot-starter
  gateway-spring-cloud-starter

sherlock-platform-services
  api-gateway
  iam-service
  tenant-service
  audit-service
```

### 模块职责

- `sherlock-framework-bom`：维护 Java、Spring Boot、Spring Cloud 及全部基础设施客户端的已验证依赖矩阵。
- `sherlock-framework-base`：纯 Java 错误码、领域异常、API 结果模型、分页模型、校验辅助类与小型工具。
- `tenant-api`：`TenantContext`、租户解析、显式系统任务上下文及传播契约。
- `security-api`：认证主体、角色、权限、令牌元数据与授权契约。
- `data-scope-api`：数据范围模型与解析器契约。
- `event-api`：中间件无关的领域事件信封与发布契约。
- `web-core`：错误响应模型、异常映射规则、Jackson 约定、请求关联及 Web 契约。
- `security-jwt`：JWT 解析、签名、刷新与吊销、Spring Security 集成及权限版本校验。
- `data-mybatis`：MyBatis 租户与数据范围拦截、持久化审计字段和结构校验。
- `cache-redis`：租户安全的缓存键构造、cache-aside 支持和有边界的分布式锁。
- `mq-rabbitmq`：RabbitMQ 事件发布、发布确认、消费者幂等、重试及死信支持。
- 平台 Starter：Nacos、OpenTelemetry、Prometheus 指标、Loki 兼容日志字段、Tempo 链路追踪和 Gateway 默认配置。

第一期不单独拆分“工具包、分页包、审计包、数据源包”等薄模块，上述能力归属各自边界。Kafka、分库分表、工作流、任务调度、多渠道通知和框架测试模块延后。

## 租户、身份与数据授权

### 身份模型

用户通过 `iam-service` 使用账号密码登录，获得短期 JWT 和刷新令牌。JWT 仅包含：

- 用户 ID；
- 租户 ID；
- 角色及权限版本；
- 令牌 ID（`jti`）；
- 过期时间以及标准签发者、受众元数据。

JWT 不保存完整部门树或完整数据范围展开结果。服务在需要计算数据范围时，从租户隔离的缓存中取得带版本的策略快照。

### 组织与数据范围

用户可以属于多个部门，管理者可管理多个部门。部门为树形结构，管理范围包含每个受管部门的全部下级部门。多角色的数据范围按集合并集计算。

第一期支持下列数据范围：

- 本人创建或本人拥有的数据；
- 指定部门的数据；
- 本部门及全部下级部门的数据；
- 当前租户内的全部数据。

“全租户数据”仅授予已授权的租户管理员，且绝不跨越租户边界。

### 请求链路

1. 网关校验 JWT，执行租户感知限流，创建关联 ID，并路由请求。
2. 目标业务服务再次校验 JWT。
3. 服务过滤器建立 `TenantContext` 与 `PrincipalContext`。
4. 应用层校验请求权限，并解析适用的数据范围并集。
5. MyBatis 追加租户与数据范围条件。
6. 返回携带关联 ID 的响应；请求、异步和消息上下文均被清理。

上下文必须传播到受支持的线程池任务与消息消费者，并在 `finally` 块中清理。缺失、格式错误、伪造或与资源不一致的租户身份必须拒绝。

### 共享表规则

所有租户管理的表必须包含非空 `tenant_id`。全部唯一索引和高频访问索引都必须将 `tenant_id` 纳入合适的复合索引，例如 `(tenant_id, created_at)`。

MyBatis 对每一个受管查询和写操作注入 `tenant_id = currentTenantId`。数据范围谓词是在租户谓词之上追加，而不是替代。启动校验发现 Mapper 声明的租户表缺少租户元数据时必须失败。原生 SQL、管理任务和不支持的查询路径必须走显式、经审查的框架逃生通道，不允许静默跳过过滤。

后台任务必须处理显式租户清单；缺少租户绝不解释为处理全部租户。系统任务使用受限的 `SystemTenantContext`，其用途必须在白名单中并产生审计记录。

## 缓存、事件、审计与故障处理

### Redis

Redis 使用 cache-aside 语义。缓存键包含业务命名空间与租户 ID，例如 `order:{tenantId}:{orderId}`。数据库事务成功提交后，失效对应缓存键。除非业务场景明确要求强缓存依赖，Redis 不可用时降级查询 MySQL。

分布式锁必须采用租户命名空间、有限租约，并在释放时验证令牌。锁只用于短时竞争协调，不能被当作事务一致性手段。

### 事件与 Outbox

所有跨服务业务事件使用稳定信封，至少包含 `eventId`、`eventType`、`occurredAt`、`tenantId`、`actorId`、`traceId`、载荷版本和载荷。

应用在同一 MySQL 事务中写入业务变更和 Outbox 记录。发布器读取待发布 Outbox 条目，通过 RabbitMQ Publisher Confirm 发布，并仅在确认后标记事件已发布。发布失败按有上限的指数退避策略重试。

消费者恢复租户与关联上下文，以 `eventId` 完成幂等记录，在本地事务内处理，并仅在成功后确认消息。可恢复失败进入重试队列；重试耗尽后进入死信队列，同时生成带租户和追踪上下文的告警。

### 审计

敏感或重要写操作产生结构化审计事件，包含租户 ID、操作人 ID、目标资源、动作、前后摘要、请求 IP、结果、时间戳和追踪 ID。主事务仅将审计事件写入 Outbox；`audit-service` 异步持久化，以避免审计存储故障阻塞主操作。

### 错误契约

Web 响应包含稳定应用错误码、用户安全消息、必要时的字段级校验错误、时间戳和 `traceId`。认证、授权、租户不匹配和数据范围拒绝使用正确的 HTTP 401、403 或 404 语义。内部异常详情与堆栈仅记录日志，绝不返回客户端。

## 可观测性

所有服务输出结构化 JSON 日志，字段包括服务名、环境、`traceId`、`spanId`、经过安全处理的租户上下文及执行结果。OpenTelemetry 链路通过网关、HTTP 客户端、线程池和 RabbitMQ 消息传播。Prometheus 采集指标，Grafana 提供仪表盘，Loki 存储日志，Tempo 存储链路。

租户 ID 和用户 ID 不得作为 Prometheus 指标标签，以避免无限基数。按租户排查使用受访问控制的日志和链路追踪。

## 配置、迁移与运维

Nacos 提供服务注册、发现和环境配置。框架配置通过类型安全的 Spring Boot 属性提供，Starter 提供合理且安全的默认值。应用特有配置保留在各服务自己的命名空间。

每个可部署服务拥有自己的 Flyway 迁移脚本与迁移历史表。框架模块只提供命名和启动校验规范，不维护共享业务库迁移流程。

## 测试与发布策略

| 测试层次 | 覆盖范围 |
| --- | --- |
| 单元测试 | 纯 Java 契约、规则、令牌及数据范围解析。 |
| Starter 测试 | 独立 Spring Boot 上下文创建、默认值、属性覆盖和功能关闭场景。 |
| 集成测试 | 使用 Testcontainers 验证 MySQL、Redis、RabbitMQ 和 Nacos 的真实协议行为。 |
| 端到端测试 | 覆盖网关、IAM、租户上下文、持久化、事件、审计及可观测性关联。 |
| 故障测试 | 覆盖事务回滚、重复事件、消费者重试、死信、缓存故障和配置不可用。 |

所有租户感知模块必须证明跨租户读、写、缓存访问和消息均被拒绝或隔离。没有独立集成测试套件的 Starter 不得发布。

采用语义化版本。BOM 仅发布经过验证的依赖组合。破坏性公共 API 变更必须进入新的主版本。配置和 API 弃用至少保留一个次版本，并提供清晰迁移说明。每个发布版本包含变更记录、兼容性矩阵、配置参考、迁移指南和可运行示例。SNAPSHOT 制品先发布到内部仓库，验证通过后再发布不可变正式版本。

## 实施顺序

1. 建立 Maven 聚合、父 POM、BOM、Java 21 质量检查、依赖架构规则、`base` 与契约测试基础。
2. 实现租户上下文、Web 约定、认证/JWT、MyBatis 租户和数据范围强制、Redis 租户缓存键规范，并用一个示例服务打通。
3. 实现 RabbitMQ、Outbox、审计事件、Nacos、Gateway、OpenTelemetry，以及 Prometheus/Loki/Tempo/Grafana 集成并验证故障路径。
4. 交付 IAM、租户、审计和网关应用、Flyway 基线、传统 VM 部署说明、示例业务服务和内部仓库发布流程。

## 延后范围

第一期不包含 Kafka、多数据源路由、分库分表、工作流、任务调度、多渠道通知、Kubernetes 部署，以及租户独立 Schema 或独立数据库。契约边界为未来适配器和路由策略预留空间，但不改变核心租户、身份、事件和数据范围模型。

# Sherlock Framework Platform Design

## Status

Approved design for the first implementation cycle.

## Purpose

Build a Java 21 and Spring Boot 3 based microservice platform for multi-tenant SaaS applications running on virtual machines or physical servers. The platform supplies reusable application dependencies and a small set of deployable control-plane services.

The design prioritizes stable, pure-Java contracts and Spring Boot starters over direct application coupling to infrastructure clients. It is a complete platform suite in delivery scope, but its internal organization follows a contract-core plus infrastructure-adapter model.

## Fixed Technology Baseline

| Area | Decision |
| --- | --- |
| Language/runtime | Java 21 |
| Application framework | Spring Boot 3.x |
| Build and publishing | Maven multi-module project |
| Relational database | MySQL 8 |
| Database migration | Flyway |
| Tenant storage | Shared database and shared tables, partitioned by `tenant_id` |
| Authentication | Built-in account/password login and JWT |
| Cache | Redis only |
| Messaging | RabbitMQ |
| Service discovery and configuration | Nacos |
| External API entry | Spring Cloud Gateway |
| Metrics | Prometheus and Grafana |
| Logs | Loki |
| Tracing | OpenTelemetry and Grafana Tempo |
| Deployment target | Virtual machines or physical servers |

## Architecture

The architecture has four one-way layers:

```text
Business services, gateway, IAM, tenant service, audit service
    -> Spring Boot starters
    -> pure Java contracts
    -> infrastructure adapters and external platforms
```

`base`, tenant, security, data-scope, event, and observability contracts never depend on Spring, MyBatis, Redis, RabbitMQ, Nacos, or an observability vendor. Starters own auto-configuration and configuration properties. Infrastructure adapters contain vendor-specific code.

Business services use the BOM and only the starters they need. They must not directly depend on Nacos, RabbitMQ, or Redis client APIs for capabilities covered by framework contracts.

### Deployable Platform Services

| Service | Responsibility |
| --- | --- |
| `api-gateway` | External entry point, JWT pre-validation, tenant-aware rate limiting, routing, and request correlation. |
| `iam-service` | Account/password login, refresh-token lifecycle, JWT issuance/revocation, tenant roles, permissions, departments, and data-scope administration. |
| `tenant-service` | Tenant lifecycle, tenant-level configuration, quotas, and tenant metadata. |
| `audit-service` | Asynchronously consumes and stores auditable actions; supports compliant search and export. |

Gateway validation is defense in depth only. Each business service independently validates JWTs and establishes its own security and tenant context. IAM is not on the synchronous request path after login; services use locally verified tokens and cached policy snapshots.

## Maven Modules

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

### Module Responsibilities

- `sherlock-framework-bom`: owns the validated dependency matrix for Java, Spring Boot, Spring Cloud, and all infrastructure clients.
- `sherlock-framework-base`: pure Java error codes, domain exceptions, API result models, pagination models, validation helpers, and small utilities.
- `tenant-api`: `TenantContext`, tenant resolution, explicit system-task context, and propagation contracts.
- `security-api`: authenticated principal, roles, permissions, token metadata, and authorization contracts.
- `data-scope-api`: data-scope model and resolver contracts.
- `event-api`: vendor-neutral domain-event envelope and publishing contracts.
- `web-core`: error response model, exception mapping rules, Jackson conventions, request correlation, and web contracts.
- `security-jwt`: JWT parsing, signing, refresh and revocation support, Spring Security integration, and policy-version checks.
- `data-mybatis`: MyBatis tenant and data-scope interception, persistence audit fields, and schema validation.
- `cache-redis`: tenant-safe cache key construction, cache-aside support, and bounded distributed locking.
- `mq-rabbitmq`: RabbitMQ event publishing, publisher confirms, consumer idempotency, retries, and dead-letter support.
- platform starters: Nacos, OpenTelemetry, Prometheus metrics, Loki-compatible logging fields, Tempo tracing, and Gateway defaults.

Do not create thin standalone utility, pagination, audit, or datasource modules during the first cycle. Keep those concerns within their owning capability. Kafka, sharding, workflow, scheduling, multi-channel notification, and a framework test module are deferred to later cycles.

## Tenant, Identity, and Data Authorization

### Identity Model

Users log in with account/password through `iam-service`. A short-lived JWT and a refresh token are issued. JWT claims include only:

- user ID;
- tenant ID;
- role and permission version;
- token ID (`jti`);
- expiration and standard issuer/audience metadata.

The token does not contain a department tree or complete data-scope expansion. Services retrieve a versioned policy snapshot from their tenant-safe cache when data-scope expansion is needed.

### Organization and Data Scope

Users may belong to multiple departments. Management rights may cover multiple managed departments. Departments form a tree, and a manager's scope includes every descendant of each managed department. Multiple roles compose by set union.

First-cycle data scopes are:

- self-created or self-owned data;
- specified departments;
- managed departments plus all descendants;
- all data within the current tenant.

The all-tenant scope is reserved for authorized tenant administrators. It never bypasses the tenant boundary.

### Request Flow

1. The gateway validates a JWT, applies tenant-aware limits, creates a correlation ID, and routes the request.
2. The target service validates the JWT again.
3. A service filter establishes `TenantContext` and `PrincipalContext`.
4. Application authorization checks the requested permission and resolves the applicable union data scope.
5. MyBatis injects tenant and data-scope constraints.
6. The response is returned with the correlation ID; request, async, and message contexts are cleared.

Contexts must propagate into supported executor tasks and message consumers, then be cleared in `finally` blocks. Missing, malformed, forged, or resource-inconsistent tenant identity is denied.

### Shared-Table Rules

Every tenant-managed table contains a non-null `tenant_id`. All unique indexes and high-volume access indexes include `tenant_id` as their leading or appropriate composite component, for example `(tenant_id, created_at)`.

MyBatis injects `tenant_id = currentTenantId` on every managed query and mutation. Data-scope predicates are added in addition to, never instead of, the tenant predicate. Startup validation fails if a mapper declares a tenant-managed table without the required tenant metadata. Raw SQL, administrative jobs, and unsupported query paths must opt into an explicit, reviewed framework escape hatch; they cannot silently skip filtering.

Jobs must process an explicit tenant list. There is no default interpretation of a missing tenant as all tenants. System tasks use a restricted `SystemTenantContext` with an allowlisted purpose and audit record.

## Cache, Events, Auditing, and Failure Handling

### Redis

Redis uses cache-aside semantics. Cache keys include the business namespace and tenant ID, for example `order:{tenantId}:{orderId}`. A successful database transaction invalidates affected keys after commit. Redis failures fall back to MySQL unless a use case explicitly requires a strong cache dependency.

Distributed locks are tenant-namespaced, time-bounded, and token-verified on release. They coordinate short-lived contention only and are never treated as transaction consistency.

### Events and Outbox

All cross-service business events use a stable envelope with `eventId`, `eventType`, `occurredAt`, `tenantId`, `actorId`, `traceId`, payload version, and payload.

The application writes the business change and an Outbox record inside the same MySQL transaction. A publisher reads pending Outbox entries, publishes with RabbitMQ publisher confirms, and marks an entry published only after confirmation. Failed publication retries with bounded exponential backoff.

Consumers restore tenant and correlation context, record `eventId` for idempotency, process inside a local transaction, and acknowledge only after success. Recoverable failures use retry queues. Exhausted messages enter a dead-letter queue and create an alert with tenant and trace context.

### Audit

Sensitive or material write operations produce structured audit events: tenant ID, actor ID, target resource, action, before/after summaries, request IP, result, timestamp, and trace ID. The primary transaction records the event in Outbox; `audit-service` persists it asynchronously so audit storage failures do not block the primary operation.

### Error Contract

Web responses contain a stable application error code, user-safe message, field-level validation errors when applicable, timestamp, and `traceId`. Authentication, authorization, tenant mismatch, and data-scope rejection use correct HTTP 401, 403, or 404 semantics. Internal exception details and stack traces are logged only and never returned to clients.

## Observability

All services emit structured JSON logs with service name, environment, `traceId`, `spanId`, tenant-safe context, and outcome. OpenTelemetry traces flow through gateway, HTTP clients, executors, and RabbitMQ messages. Metrics use Prometheus and dashboards use Grafana. Logs are stored in Loki and traces in Tempo.

Tenant IDs and user IDs are not Prometheus labels because they create unbounded cardinality. Per-tenant investigation uses logs and traces subject to tenant-safe access controls.

## Configuration, Migration, and Operations

Nacos provides service registration, discovery, and environment configuration. Framework configuration is exposed through typed Spring Boot properties, and reasonable secure defaults are provided by starters. Application-specific configuration stays in the service namespace.

Each deployable service owns Flyway migrations and its own migration history table. Framework modules provide naming and startup validation conventions, not a shared business-schema migration process.

## Testing and Release Policy

| Test layer | Scope |
| --- | --- |
| Unit tests | Pure Java contracts, rules, token and data-scope resolution. |
| Starter tests | Isolated Spring Boot context creation, defaults, property overrides, and disabled-feature behavior. |
| Integration tests | Testcontainers for MySQL, Redis, RabbitMQ, and Nacos; verifies actual protocol behavior. |
| End-to-end tests | Gateway, IAM, tenant context, persistence, events, auditing, and observability correlation. |
| Failure tests | Transaction rollback, duplicated events, consumer retries, dead-lettering, cache outages, and configuration unavailability. |

Every tenant-aware module must prove that cross-tenant reads, writes, cache access, and messages are denied or isolated. No starter release is allowed without its independent integration test suite.

Use semantic versioning. The BOM publishes only a tested dependency combination. Breaking public API changes require a new major version. Configuration and API deprecations remain supported for at least one minor version with clear migration notes. Releases include a changelog, compatibility matrix, configuration reference, migration guide, and runnable example. SNAPSHOT artifacts publish internally first; immutable releases publish only after verification.

## Delivery Sequence

1. Establish Maven aggregation, parent POM, BOM, Java 21 quality checks, dependency architecture rules, `base`, and contract-testing foundations.
2. Implement tenant context, web conventions, security/JWT, MyBatis tenant and data-scope enforcement, Redis tenant key conventions, and one example service.
3. Implement RabbitMQ, Outbox, audit events, Nacos, Gateway, OpenTelemetry, and the Prometheus/Loki/Tempo/Grafana integration with failure-path verification.
4. Deliver IAM, tenant, audit, and gateway applications, Flyway baselines, VM deployment guidance, sample business service, and internal repository release process.

## Deferred Scope

The first implementation cycle excludes Kafka, multi-datasource routing, database sharding, workflow, scheduling, notification channels, Kubernetes deployment, and tenant-specific schema or database isolation. The contract boundaries intentionally permit future adapters and routing strategies without changing core tenant, identity, event, or data-scope models.

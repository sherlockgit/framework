# sherlock-cloud

## Build Requirements

- Java 21 or later
- Docker, for the MySQL and Redis Testcontainers end-to-end suite

Run the complete build with the Maven Wrapper:

```powershell
.\mvnw.cmd verify
```

The tenant demo's end-to-end test starts MySQL 8 and Redis 7 containers and can be run directly in a Docker-enabled environment:

```powershell
.\mvnw.cmd -pl examples/tenant-demo -am test "-Dtest=TenantIsolationE2ETest"
```

Without Docker, the real MySQL and Redis Testcontainers end-to-end class is skipped. The demo's H2 test is only a supplementary application-context and Mapper regression: it verifies Flyway, bean wiring, and framework-injected tenant IDs for Mapper inserts. It does not validate HTTP behavior, JWT authentication, security filters, Redis, or end-to-end tenant isolation, and cannot replace the Docker-based MySQL and Redis acceptance test.

## Tenant Demo Acceptance

`examples/tenant-demo` exposes only order creation and lookup by business key. The end-to-end scenario creates `same-business-key` for tenant 1 and tenant 2 with different names, then verifies that each Bearer JWT reads its own HTTP/MyBatis result and that Redis contains distinct tenant cache keys. It also verifies missing permissions, missing tenant claims, and self-only data scope rejection.

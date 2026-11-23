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

Without Docker, the Testcontainers class is skipped while the demo's H2 application-context test continues to verify Flyway, MyBatis mapping, security filters, and controller wiring.

## Tenant Demo Acceptance

`examples/tenant-demo` exposes only order creation and lookup by business key. The end-to-end scenario creates `same-business-key` for tenant 1 and tenant 2 with different names, then verifies that each Bearer JWT reads its own HTTP/MyBatis result and that Redis contains distinct tenant cache keys. It also verifies missing permissions, missing tenant claims, and self-only data scope rejection.

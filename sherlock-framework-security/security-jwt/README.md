# security-jwt

提供不依赖 Spring 的 `JwtTokenService` 契约及 Nimbus RS256 实现，用于签发和直接验证框架 JWT。

令牌包含 `sub`、`tenant_id`、`permissions`、`policy_version`、`jti`、签发者、受众、有效期和可选生效时间；不包含部门树。验证签名、算法、签发者、受众、过期和 `nbf`，时钟偏差固定为零。

生产类设计见 [docs/classes.md](docs/classes.md)。

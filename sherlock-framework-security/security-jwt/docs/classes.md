# security-jwt 类说明

## `com.sherlock.framework.security.jwt`

### `JwtClaims`

签发前后的框架 JWT 声明模型。保存用户、租户、权限、策略版本和令牌 ID；权限集合不可变，租户和用户必须有效。

### `JwtTokenService`

JWT 签发与验证契约。实现应保证 `issue` 仅接受正有效期，`verify` 对签名或声明失败统一拒绝。

### `NimbusJwtTokenService`

基于 Nimbus 的 RS256 实现。使用私钥签发、公钥验证，严格校验算法、签名、签发者、受众、过期时间和 `nbf`，不接受非 RS256 令牌。

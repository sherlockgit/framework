package com.sherlock.framework.example;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sherlock.framework.security.jwt.JwtClaims;
import com.sherlock.framework.security.jwt.NimbusJwtTokenService;
import com.sherlock.framework.tenant.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = TenantDemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.context.annotation.Import(TenantIsolationE2ETest.JwtDecoderConfiguration.class)
class TenantIsolationE2ETest {

    private static final String ISSUER = "tenant-demo";
    private static final String AUDIENCE = "tenant-demo-api";
    private static final KeyPair KEY_PAIR = keyPair();

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.4")
            .withDatabaseName("tenant_demo")
            .withUsername("tenant_demo")
            .withPassword("tenant_demo");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4.2-alpine")
            .withExposedPorts(6379);

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private TestRestTemplate client;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("sherlock.security.jwt.issuer", () -> ISSUER);
        registry.add("sherlock.security.jwt.audience", () -> AUDIENCE);
    }

    @BeforeEach
    void setUpCache() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void tenantOneCannotReadTenantTwoOrderThroughHttpMyBatisOrCache() throws Exception {
        String tenantOne = tokenFor(1L, 101L, Set.of("order:read", "order:write"));
        String tenantTwo = tokenFor(2L, 201L, Set.of("order:read", "order:write"));

        assertThat(createOrder(tenantOne, "same-business-key", "one").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createOrder(tenantTwo, "same-business-key", "two").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(getOrder(tenantOne, "same-business-key").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(nameOf(getOrder(tenantOne, "same-business-key").getBody())).isEqualTo("one");
        assertThat(getOrder(tenantTwo, "same-business-key").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(nameOf(getOrder(tenantTwo, "same-business-key").getBody())).isEqualTo("two");

        assertThat(redis.opsForValue().get("order:1:same-business-key")).isEqualTo("one");
        assertThat(redis.opsForValue().get("order:2:same-business-key")).isEqualTo("two");
    }

    @Test
    void securityMissingTenantAndSelfDataScopeAreEnforced() {
        String owner = tokenFor(1L, 101L, Set.of("order:read", "order:write"));
        String otherUser = tokenFor(1L, 102L, Set.of("order:read"));

        assertThat(createOrder(owner, "private-order", "private").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(getOrder(otherUser, "private-order").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(createOrder(tokenFor(1L, 101L, Set.of("order:read")), "denied", "denied").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getOrder(null, "private-order").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getOrder(tokenWithoutTenant(), "private-order").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private org.springframework.http.ResponseEntity<String> createOrder(String token, String businessKey, String name) {
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return client.postForEntity("/orders", new HttpEntity<>(
                Map.of("businessKey", businessKey, "departmentId", 10, "name", name), headers), String.class);
    }

    private org.springframework.http.ResponseEntity<String> getOrder(String token, String businessKey) {
        return client.exchange("/orders/{businessKey}", org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class, businessKey);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private String nameOf(String body) throws Exception {
        JsonNode json = objectMapper.readTree(body);
        return json.at("/data/name").asText();
    }

    private static String tokenFor(long tenantId, long userId, Set<String> permissions) {
        return new NimbusJwtTokenService(
                (RSAPrivateKey) KEY_PAIR.getPrivate(), (RSAPublicKey) KEY_PAIR.getPublic(), ISSUER, AUDIENCE,
                Clock.systemUTC())
                .issue(new JwtClaims(userId, new TenantId(tenantId), permissions, 1L, "token-" + tenantId + '-' + userId),
                        Duration.ofMinutes(10));
    }

    private static String tokenWithoutTenant() {
        try {
            Instant now = Instant.now();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
                    .subject("101").issuer(ISSUER).audience(AUDIENCE).jwtID("no-tenant")
                    .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(600)))
                    .claim("permissions", List.of("order:read")).claim("policy_version", 1L).build());
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
            return jwt.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static KeyPair keyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration
    static class JwtDecoderConfiguration {

        @Bean
        NimbusJwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEY_PAIR.getPublic()).build();
        }

    }
}

package com.sherlock.framework.security.jwt;

import com.sherlock.framework.tenant.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NimbusJwtTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T00:00:00Z");
    private KeyPair keyPair;

    @BeforeEach
    void generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void issuedTokenContainsTenantAndPolicyVersion() {
        NimbusJwtTokenService service = serviceAt(NOW, "framework", "framework-api");
        JwtClaims claims = new JwtClaims(7L, new TenantId(9L), Set.of("order:read"), 3L, "jti-1");

        String token = service.issue(claims, Duration.ofMinutes(15));
        JwtClaims parsed = service.verify(token);

        assertThat(parsed.userId()).isEqualTo(7L);
        assertThat(parsed.tenantId().value()).isEqualTo(9L);
        assertThat(parsed.permissions()).containsExactly("order:read");
        assertThat(parsed.policyVersion()).isEqualTo(3L);
        assertThat(parsed.tokenId()).isEqualTo("jti-1");
    }

    @Test
    void expiredTokenIsRejected() {
        String token = serviceAt(NOW, "framework", "framework-api")
                .issue(new JwtClaims(7L, new TenantId(9L), Set.of("order:read"), 3L, "jti-1"), Duration.ofMinutes(1));

        assertThatThrownBy(() -> serviceAt(NOW.plusSeconds(61), "framework", "framework-api").verify(token))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void tokenForWrongAudienceIsRejected() {
        String token = serviceAt(NOW, "framework", "another-api")
                .issue(new JwtClaims(7L, new TenantId(9L), Set.of("order:read"), 3L, "jti-1"), Duration.ofMinutes(1));

        assertThatThrownBy(() -> serviceAt(NOW, "framework", "framework-api").verify(token))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void tokenFromWrongIssuerIsRejected() {
        String token = serviceAt(NOW, "another-issuer", "framework-api")
                .issue(new JwtClaims(7L, new TenantId(9L), Set.of("order:read"), 3L, "jti-1"), Duration.ofMinutes(1));

        assertThatThrownBy(() -> serviceAt(NOW, "framework", "framework-api").verify(token))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void tokenWithTamperedSignatureIsRejected() {
        NimbusJwtTokenService service = serviceAt(NOW, "framework", "framework-api");
        String token = service.issue(new JwtClaims(7L, new TenantId(9L), Set.of("order:read"), 3L, "jti-1"), Duration.ofMinutes(1));
        int signatureStart = token.lastIndexOf('.') + 1;
        char originalSignatureCharacter = token.charAt(signatureStart);
        char replacementSignatureCharacter = originalSignatureCharacter == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart) + replacementSignatureCharacter
                + token.substring(signatureStart + 1);

        assertThatThrownBy(() -> service.verify(tampered))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void tokenWithFutureNotBeforeTimeIsRejected() throws Exception {
        NimbusJwtTokenService service = serviceAt(NOW, "framework", "framework-api");
        String token = tokenWithNotBefore(NOW.plusSeconds(60));

        assertThatThrownBy(() -> service.verify(token))
                .isInstanceOf(SecurityException.class);
    }

    private NimbusJwtTokenService serviceAt(Instant instant, String issuer, String audience) {
        return new NimbusJwtTokenService(
                (RSAPrivateKey) keyPair.getPrivate(),
                (RSAPublicKey) keyPair.getPublic(),
                issuer,
                audience,
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    private String tokenWithNotBefore(Instant notBefore) throws Exception {
        com.nimbusds.jwt.JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .subject("7")
                .issuer("framework")
                .audience("framework-api")
                .jwtID("jti-1")
                .claim("tenant_id", 9L)
                .claim("permissions", Set.of("order:read"))
                .claim("policy_version", 3L)
                .notBeforeTime(java.util.Date.from(notBefore))
                .expirationTime(java.util.Date.from(NOW.plusSeconds(300)))
                .build();
        com.nimbusds.jwt.SignedJWT signedJwt = new com.nimbusds.jwt.SignedJWT(
                new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256).build(), claims);
        signedJwt.sign(new com.nimbusds.jose.crypto.RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
        return signedJwt.serialize();
    }
}

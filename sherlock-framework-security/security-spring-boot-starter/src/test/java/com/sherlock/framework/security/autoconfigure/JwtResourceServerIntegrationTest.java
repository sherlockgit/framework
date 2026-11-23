package com.sherlock.framework.security.autoconfigure;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sherlock.framework.security.PrincipalContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JwtResourceServerIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "sherlock.security.jwt.issuer=framework-issuer",
        "sherlock.security.jwt.audience=framework-api"
})
class JwtResourceServerIntegrationTest {

    private static final KeyPair KEY_PAIR = createKeyPair();

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void bearerJwtEstablishesFrameworkPrincipalContext() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer " + token(TokenOptions.valid())))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void bearerJwtWithZeroPolicyVersionIsAccepted() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer "
                        + token(TokenOptions.withPolicyVersion(0L))))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void bearerJwtPermissionsAreAvailableAsAuthoritiesWithoutScopePrefix() throws Exception {
        mockMvc.perform(get("/permission").header("Authorization", "Bearer " + token(TokenOptions.valid())))
                .andExpect(status().isOk())
                .andExpect(content().string("allowed"));
    }

    @Test
    void bearerJwtWithoutRequiredPermissionIsRejectedWithForbidden() throws Exception {
        mockMvc.perform(get("/permission").header("Authorization", "Bearer " + token(TokenOptions.withPermissions(List.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void bearerJwtWithTamperedSignatureIsRejectedWithUnauthorized() throws Exception {
        String token = token(TokenOptions.valid());
        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        String tampered = token.substring(0, signatureStart) + (original == 'A' ? 'B' : 'A')
                + token.substring(signatureStart + 1);

        expectUnauthorized(tampered);
    }

    @Test
    void bearerJwtWithNonRs256AlgorithmIsRejectedWithUnauthorized() throws Exception {
        SignedJWT signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).build(), claims(TokenOptions.valid()));
        signedJwt.sign(new MACSigner("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));

        expectUnauthorized(signedJwt.serialize());
    }

    @Test
    void bearerJwtWithWrongIssuerIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withIssuer("another-issuer")));
    }

    @Test
    void bearerJwtWithWrongAudienceIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withAudience("another-api")));
    }

    @Test
    void bearerJwtWithExpiredExpiryIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withExpiration(Instant.now().minusSeconds(30))));
    }

    @Test
    void bearerJwtWithFutureNotBeforeTimeIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withNotBefore(Instant.now().plusSeconds(30))));
    }

    @Test
    void bearerJwtWithInvalidPrincipalClaimsIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withSubject("not-a-number")));
    }

    @Test
    void bearerJwtWithInvalidTenantClaimIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withTenantId("not-a-number")));
    }

    @Test
    void bearerJwtWithInvalidPolicyVersionClaimIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withPolicyVersion("not-a-number")));
    }

    @Test
    void bearerJwtWithNegativePolicyVersionClaimIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withPolicyVersion(-1L)));
    }

    @Test
    void bearerJwtWithFractionalPolicyVersionClaimIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withPolicyVersion(3.5)));
    }

    @Test
    void bearerJwtWithFractionalTenantClaimIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withTenantId(9.5)));
    }

    @Test
    void bearerJwtWithMissingTenantClaimIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withTenantId(null)));
    }

    @Test
    void bearerJwtWithMissingSubjectIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withSubject(null)));
    }

    @Test
    void bearerJwtWithMissingPolicyVersionClaimIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized(token(TokenOptions.withPolicyVersion(null)));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityAutoConfiguration.class, PrincipalController.class, DecoderConfiguration.class})
    static class TestApplication {
    }

    @TestConfiguration
    static class DecoderConfiguration {

        @Bean
        JwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEY_PAIR.getPublic()).build();
        }
    }

    @RestController
    static class PrincipalController {

        @GetMapping("/principal")
        String principalId() {
            return Long.toString(PrincipalContext.require().userId());
        }

        @GetMapping("/permission")
        @PreAuthorize("hasAuthority('order:read')")
        String permission() {
            return "allowed";
        }
    }

    private void expectUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private static String token(TokenOptions options) throws Exception {
        SignedJWT signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), claims(options));
        signedJwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
        return signedJwt.serialize();
    }

    private static JWTClaimsSet claims(TokenOptions options) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(options.issuer())
                .audience(options.audience())
                .claim("permissions", options.permissions())
                .jwtID("jti-1")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(options.expiration()));
        if (options.subject() != null) {
            builder.subject(options.subject());
        }
        if (options.tenantId() != null) {
            builder.claim("tenant_id", options.tenantId());
        }
        if (options.policyVersion() != null) {
            builder.claim("policy_version", options.policyVersion());
        }
        if (options.notBefore() != null) {
            builder.notBeforeTime(Date.from(options.notBefore()));
        }
        return builder.build();
    }

    private static KeyPair createKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create RSA key pair", exception);
        }
    }

    private record TokenOptions(
            String issuer,
            String audience,
            String subject,
            Object tenantId,
            Object policyVersion,
            List<String> permissions,
            Instant expiration,
            Instant notBefore) {

        static TokenOptions valid() {
            return new TokenOptions(
                    "framework-issuer", "framework-api", "7", 9L, 3L, List.of("order:read"),
                    Instant.now().plusSeconds(300), null);
        }

        static TokenOptions withPermissions(List<String> permissions) {
            TokenOptions valid = valid();
            return new TokenOptions(valid.issuer(), valid.audience(), valid.subject(), valid.tenantId(),
                    valid.policyVersion(), permissions, valid.expiration(), valid.notBefore());
        }

        static TokenOptions withIssuer(String issuer) {
            TokenOptions valid = valid();
            return new TokenOptions(issuer, valid.audience(), valid.subject(), valid.tenantId(),
                    valid.policyVersion(), valid.permissions(), valid.expiration(), valid.notBefore());
        }

        static TokenOptions withAudience(String audience) {
            TokenOptions valid = valid();
            return new TokenOptions(valid.issuer(), audience, valid.subject(), valid.tenantId(),
                    valid.policyVersion(), valid.permissions(), valid.expiration(), valid.notBefore());
        }

        static TokenOptions withSubject(String subject) {
            TokenOptions valid = valid();
            return new TokenOptions(valid.issuer(), valid.audience(), subject, valid.tenantId(),
                    valid.policyVersion(), valid.permissions(), valid.expiration(), valid.notBefore());
        }

        static TokenOptions withTenantId(Object tenantId) {
            TokenOptions valid = valid();
            return new TokenOptions(valid.issuer(), valid.audience(), valid.subject(), tenantId,
                    valid.policyVersion(), valid.permissions(), valid.expiration(), valid.notBefore());
        }

        static TokenOptions withPolicyVersion(Object policyVersion) {
            TokenOptions valid = valid();
            return new TokenOptions(valid.issuer(), valid.audience(), valid.subject(), valid.tenantId(),
                    policyVersion, valid.permissions(), valid.expiration(), valid.notBefore());
        }

        static TokenOptions withExpiration(Instant expiration) {
            TokenOptions valid = valid();
            return new TokenOptions(valid.issuer(), valid.audience(), valid.subject(), valid.tenantId(),
                    valid.policyVersion(), valid.permissions(), expiration, valid.notBefore());
        }

        static TokenOptions withNotBefore(Instant notBefore) {
            TokenOptions valid = valid();
            return new TokenOptions(valid.issuer(), valid.audience(), valid.subject(), valid.tenantId(),
                    valid.policyVersion(), valid.permissions(), valid.expiration(), notBefore);
        }
    }
}

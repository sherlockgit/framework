package com.sherlock.framework.security.autoconfigure;

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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JwtResourceServerIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
class JwtResourceServerIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void bearerJwtEstablishesFrameworkPrincipalContext() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer read-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void bearerJwtPermissionsAreAvailableAsAuthoritiesWithoutScopePrefix() throws Exception {
        mockMvc.perform(get("/permission").header("Authorization", "Bearer read-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("allowed"));
    }

    @Test
    void bearerJwtWithoutRequiredPermissionIsRejectedWithForbidden() throws Exception {
        mockMvc.perform(get("/permission").header("Authorization", "Bearer no-permission-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bearerJwtWithInvalidPrincipalClaimsIsRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer invalid-subject-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerJwtWithInvalidTenantClaimIsRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer invalid-tenant-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerJwtWithInvalidPolicyVersionClaimIsRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer invalid-policy-version-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerJwtWithMissingTenantClaimIsRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer missing-tenant-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerJwtWithMissingSubjectIsRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer missing-subject-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerJwtWithMissingPolicyVersionClaimIsRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer missing-policy-version-token"))
                .andExpect(status().isUnauthorized());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityAutoConfiguration.class, PrincipalController.class, JwtDecoderConfiguration.class})
    static class TestApplication {
    }

    @TestConfiguration
    static class JwtDecoderConfiguration {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> jwt(
                    token,
                    "invalid-subject-token".equals(token) ? "not-a-number" :
                            "missing-subject-token".equals(token) ? null : "7",
                    "invalid-tenant-token".equals(token) ? "not-a-number" :
                            "missing-tenant-token".equals(token) ? null : 9L,
                    "invalid-policy-version-token".equals(token) ? "not-a-number" :
                            "missing-policy-version-token".equals(token) ? null : 3L,
                    "no-permission-token".equals(token) ? List.of() : List.of("order:read"));
        }

        private Jwt jwt(String token, String subject, Object tenantId, Object policyVersion, List<String> permissions) {
            Jwt.Builder builder = Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .claim("permissions", permissions)
                    .claim("jti", "jti-1")
                    .issuedAt(Instant.parse("2026-11-23T00:00:00Z"))
                    .expiresAt(Instant.parse("2026-11-23T01:00:00Z"));
            if (subject != null) {
                builder.subject(subject);
            }
            if (tenantId != null) {
                builder.claim("tenant_id", tenantId);
            }
            if (policyVersion != null) {
                builder.claim("policy_version", policyVersion);
            }
            return builder.build();
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
}

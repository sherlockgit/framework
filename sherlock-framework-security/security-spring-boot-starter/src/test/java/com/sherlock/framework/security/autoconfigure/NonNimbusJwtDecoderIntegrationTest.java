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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NonNimbusJwtDecoderIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "sherlock.security.jwt.issuer=framework-issuer",
        "sherlock.security.jwt.audience=framework-api"
})
class NonNimbusJwtDecoderIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void validJwtFromNonNimbusDecoderIsAccepted() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void wrongAudienceFromNonNimbusDecoderIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized("wrong-audience");
    }

    @Test
    void fractionalTenantFromNonNimbusDecoderIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized("fractional-tenant");
    }

    @Test
    void futureNotBeforeFromNonNimbusDecoderIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized("future-not-before");
    }

    @Test
    void missingJtiFromNonNimbusDecoderIsRejectedWithUnauthorized() throws Exception {
        expectUnauthorized("missing-jti");
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
            return token -> jwt(token);
        }
    }

    @RestController
    static class PrincipalController {

        @GetMapping("/principal")
        String principalId() {
            return Long.toString(PrincipalContext.require().userId());
        }
    }

    private void expectUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private static Jwt jwt(String token) {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .issuer("framework-issuer")
                .audience(List.of("framework-api"))
                .subject("7")
                .claim("jti", "jti-1")
                .claim("permissions", List.of("order:read"))
                .claim("tenant_id", 9L)
                .claim("policy_version", 3L)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));
        if ("wrong-audience".equals(token)) {
            builder.audience(List.of("other-api"));
        }
        if ("fractional-tenant".equals(token)) {
            builder.claim("tenant_id", 9.5D);
        }
        if ("future-not-before".equals(token)) {
            builder.notBefore(now.plusSeconds(30));
        }
        if ("missing-jti".equals(token)) {
            builder.claims(claims -> claims.remove("jti"));
        }
        return builder.build();
    }
}

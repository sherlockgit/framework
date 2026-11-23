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
        mockMvc.perform(get("/principal").header("Authorization", "Bearer signed-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
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
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .subject("7")
                    .claim("tenant_id", 9L)
                    .claim("permissions", List.of("order:read"))
                    .claim("policy_version", 3L)
                    .claim("jti", "jti-1")
                    .issuedAt(Instant.parse("2026-11-23T00:00:00Z"))
                    .expiresAt(Instant.parse("2026-11-23T01:00:00Z"))
                    .build();
        }
    }

    @RestController
    static class PrincipalController {

        @GetMapping("/principal")
        String principalId() {
            return Long.toString(PrincipalContext.require().userId());
        }
    }
}

package com.sherlock.framework.security.autoconfigure;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sherlock.framework.security.PrincipalContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;
import java.net.InetSocketAddress;
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

@SpringBootTest(classes = JwkSetUriJwtDecoderIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
class JwkSetUriJwtDecoderIntegrationTest {

    private static final String KEY_ID = "starter-jwk";
    private static final KeyPair KEY_PAIR = createKeyPair();
    private static final HttpServer JWK_SET_SERVER = createJwkSetServer();

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    private static HttpServer createJwkSetServer() {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to start JWK set server", exception);
        }
        String body = new JWKSet(new RSAKey.Builder((RSAPublicKey) KEY_PAIR.getPublic()).keyID(KEY_ID).build()).toString();
        server.createContext("/jwks", exchange -> {
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        return server;
    }

    @AfterAll
    static void stopJwkSetServer() {
        JWK_SET_SERVER.stop(0);
    }

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("sherlock.security.jwt.issuer", () -> "framework-issuer");
        registry.add("sherlock.security.jwt.audience", () -> "framework-api");
        registry.add("sherlock.security.jwt.jwk-set-uri",
                () -> "http://localhost:" + JWK_SET_SERVER.getAddress().getPort() + "/jwks");
    }

    @Test
    void starterProvidedJwkSetDecoderAuthenticatesValidJwt() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer " + token("framework-api")))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void starterProvidedJwkSetDecoderRejectsInvalidAudience() throws Exception {
        mockMvc.perform(get("/principal").header("Authorization", "Bearer " + token("other-api")))
                .andExpect(status().isUnauthorized());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityAutoConfiguration.class, PrincipalController.class})
    static class TestApplication {
    }

    @RestController
    static class PrincipalController {

        @GetMapping("/principal")
        String principalId() {
            return Long.toString(PrincipalContext.require().userId());
        }
    }

    private static String token(String audience) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("framework-issuer")
                .audience(audience)
                .subject("7")
                .jwtID("jti-1")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("tenant_id", 9L)
                .claim("policy_version", 3L)
                .claim("permissions", List.of("order:read"))
                .build();
        SignedJWT signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims);
        signedJwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
        return signedJwt.serialize();
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
}

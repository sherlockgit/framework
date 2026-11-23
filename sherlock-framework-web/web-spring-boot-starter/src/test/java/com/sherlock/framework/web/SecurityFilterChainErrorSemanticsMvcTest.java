package com.sherlock.framework.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityFilterChainErrorSemanticsMvcTest.TestApplication.class)
@AutoConfigureMockMvc
class SecurityFilterChainErrorSemanticsMvcTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void unauthenticatedRequestKeepsSecurityUnauthorizedResponse() throws Exception {
        mvc.perform(get("/secured"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("SYS-500-001"))));
    }

    @Test
    @WithMockUser
    void unauthorizedRequestKeepsSecurityForbiddenResponse() throws Exception {
        mvc.perform(get("/secured"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("SYS-500-001"))));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecuredController.class, SecurityConfiguration.class})
    static class TestApplication {
    }

    @RestController
    static class SecuredController {

        @GetMapping("/secured")
        String secured() {
            return "secured";
        }
    }

    static class SecurityConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(requests -> requests
                            .requestMatchers("/secured").hasAuthority("order:read")
                            .anyRequest().permitAll())
                    .httpBasic(basic -> { })
                    .build();
        }
    }
}

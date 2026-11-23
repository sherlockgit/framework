package com.sherlock.framework.security.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityAutoConfigurationMvcTest.TestApplication.class)
@AutoConfigureMockMvc
class SecurityAutoConfigurationMvcTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void unauthenticatedRequestIsRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(get("/secured"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "order:read")
    void authenticatedRequestWithoutRequiredPermissionIsRejectedWithForbidden() throws Exception {
        mockMvc.perform(get("/secured"))
                .andExpect(status().isForbidden());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityAutoConfiguration.class, SecuredController.class})
    static class TestApplication {
    }

    @RestController
    static class SecuredController {

        @GetMapping("/secured")
        @PreAuthorize("hasAuthority('order:write')")
        String secured() {
            return "secured";
        }
    }
}

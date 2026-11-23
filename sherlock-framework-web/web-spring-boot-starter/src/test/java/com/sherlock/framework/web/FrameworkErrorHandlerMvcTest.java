package com.sherlock.framework.web;

import com.sherlock.framework.base.error.BusinessException;
import com.sherlock.framework.base.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FrameworkErrorHandlerMvcTest.TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class FrameworkErrorHandlerMvcTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void businessExceptionReturnsStableJsonWithoutStackTrace() throws Exception {
        mvc.perform(get("/test/business-error").header("X-Trace-Id", "trace-9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TENANT-400-001"))
                .andExpect(jsonPath("$.traceId").value("trace-9"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void invalidTraceIdIsReplacedInsteadOfReflected() throws Exception {
        mvc.perform(get("/test/business-error").header("X-Trace-Id", "bad trace id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[A-Za-z0-9-]{1,64}")))
                .andExpect(jsonPath("$.traceId").value(not("bad trace id")));
    }

    @Test
    void authenticationExceptionReturnsUnauthorizedApiResponse() throws Exception {
        mvc.perform(get("/test/authentication-error"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-401-001"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void accessDeniedExceptionReturnsForbiddenApiResponse() throws Exception {
        mvc.perform(get("/test/access-denied-error"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH-403-001"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void unknownExceptionReturnsSafeInternalServerError() throws Exception {
        mvc.perform(get("/test/unknown-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SYS-500-001"))
                .andExpect(jsonPath("$.message").value("System error"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("secret failure detail"))));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(ErrorController.class)
    static class TestApplication {
    }

    @RestController
    static class ErrorController {

        @GetMapping("/test/business-error")
        String businessError() {
            throw new BusinessException(new ErrorCode("TENANT-400-001", "Tenant request is invalid"));
        }

        @GetMapping("/test/authentication-error")
        String authenticationError() {
            throw new AuthenticationCredentialsNotFoundException("credentials missing");
        }

        @GetMapping("/test/access-denied-error")
        String accessDeniedError() {
            throw new AccessDeniedException("access denied");
        }

        @GetMapping("/test/unknown-error")
        String unknownError() {
            throw new IllegalStateException("secret failure detail");
        }
    }

}

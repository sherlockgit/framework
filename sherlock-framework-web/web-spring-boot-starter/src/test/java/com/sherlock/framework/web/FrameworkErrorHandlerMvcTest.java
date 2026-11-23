package com.sherlock.framework.web;

import com.sherlock.framework.base.error.BusinessException;
import com.sherlock.framework.base.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FrameworkErrorHandlerMvcTest.TestApplication.class)
@AutoConfigureMockMvc
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
                .andExpect(header().string("X-Trace-Id", "trace-9"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void invalidTraceIdIsReplacedInsteadOfReflected() throws Exception {
        mvc.perform(get("/test/business-error").header("X-Trace-Id", "bad trace id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[A-Za-z0-9-]{1,64}")))
                .andExpect(jsonPath("$.traceId").value(not("bad trace id")))
                .andExpect(header().string("X-Trace-Id", matchesPattern("[A-Za-z0-9-]{1,64}")))
                .andExpect(header().string("X-Trace-Id", not("bad trace id")));
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

    @Test
    void responseStatusExceptionKeepsItsClientStatusWithoutExposingReason() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HTTP-404-001"))
                .andExpect(jsonPath("$.message").value("Resource not found"))
                .andExpect(jsonPath("$.code").value(not("SYS-500-001")))
                .andExpect(content().string(not(containsString("sensitive internal diagnostic"))));
    }

    @Test
    void responseStatusAnnotatedExceptionKeepsStatusFromItsSuperclass() throws Exception {
        mvc.perform(get("/test/annotated-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HTTP-404-001"))
                .andExpect(jsonPath("$.code").value(not("SYS-500-001")));
    }

    @Test
    void methodNotSupportedPreservesAllowHeader() throws Exception {
        mvc.perform(post("/test/get-only"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.code").value("HTTP-405-001"));
    }

    @Test
    void malformedJsonReturnsStableBadRequestResponse() throws Exception {
        mvc.perform(post("/test/payload")
                        .contentType("application/json")
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEB-400-001"))
                .andExpect(jsonPath("$.code").value(not("SYS-500-001")));
    }

    @Test
    void invalidRequestBodyReturnsBadRequestWithSafeFieldErrors() throws Exception {
        mvc.perform(post("/test/payload")
                        .contentType("application/json")
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEB-400-002"))
                .andExpect(jsonPath("$.data[0].field").value("name"))
                .andExpect(jsonPath("$.data[0].message").value("Invalid value"))
                .andExpect(jsonPath("$.code").value(not("SYS-500-001")));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(ErrorController.class)
    static class TestApplication {

        @Bean
        SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                    .build();
        }
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

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                    "sensitive internal diagnostic");
        }

        @GetMapping("/test/annotated-not-found")
        String annotatedNotFound() {
            throw new ChildNotFoundException();
        }

        @GetMapping("/test/get-only")
        String getOnly() {
            return "available via GET";
        }

        @PostMapping("/test/payload")
        Payload payload(@Valid @RequestBody Payload payload) {
            return payload;
        }
    }

    record Payload(@NotBlank String name) {
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class ParentNotFoundException extends RuntimeException {
    }

    static class ChildNotFoundException extends ParentNotFoundException {
    }

}

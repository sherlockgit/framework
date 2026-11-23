# HTTP Error Semantics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve framework HTTP semantics and response headers while returning a stable, non-sensitive API error body.

**Architecture:** `FrameworkErrorHandler` remains the one API error-body boundary. It will derive a status from Spring `ErrorResponse` or a class-hierarchy `@ResponseStatus`, map that status to a controlled message, and construct `ResponseEntity` values with copied framework headers. MVC integration tests exercise actual controller dispatch so routing and resolver behavior are covered.

**Tech Stack:** Java 21, Spring Framework 6.2 / Spring Boot 3.4, MockMvc, JUnit 5.

---

### Task 1: Lock down safe `ResponseStatusException` messages

**Files:**
- Modify: `sherlock-framework-web/web-spring-boot-starter/src/test/java/com/sherlock/framework/web/FrameworkErrorHandlerMvcTest.java`
- Modify: `sherlock-framework-web/web-core/src/main/java/com/sherlock/framework/web/error/FrameworkErrorHandler.java`

- [x] **Step 1: Write the failing MVC test**

```java
@Test
void responseStatusExceptionDoesNotExposeItsReason() throws Exception {
    mvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Resource not found"))
            .andExpect(content().string(not(containsString("sensitive internal reason"))));
}
```

- [x] **Step 2: Run the MVC test to verify it fails**

Run: `mvn -pl sherlock-framework-web/web-spring-boot-starter -am -Dtest=FrameworkErrorHandlerMvcTest#responseStatusExceptionDoesNotExposeItsReason test`

Expected: FAIL because the handler reflects the reason.

- [x] **Step 3: Implement the controlled status-message mapping**

```java
private String messageFor(HttpStatusCode status) {
    return switch (status.value()) {
        case 400 -> "Invalid request parameters";
        case 404 -> "Resource not found";
        default -> "Request could not be processed";
    };
}
```

Use this mapping rather than `ResponseStatusException.getReason()`.

- [x] **Step 4: Re-run the test to verify it passes**

Run the command from Step 2. Expected: PASS.

### Task 2: Preserve status annotations and Spring error headers

**Files:**
- Modify: `sherlock-framework-web/web-spring-boot-starter/src/test/java/com/sherlock/framework/web/FrameworkErrorHandlerMvcTest.java`
- Modify: `sherlock-framework-web/web-core/src/main/java/com/sherlock/framework/web/error/FrameworkErrorHandler.java`

- [x] **Step 1: Write failing real MVC tests**

```java
@Test
void responseStatusAnnotatedExceptionKeepsNotFoundStatus() throws Exception {
    mvc.perform(get("/test/annotated-not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HTTP-404-001"))
            .andExpect(jsonPath("$.code").value(not("SYS-500-001")));
}

@Test
void methodNotSupportedPreservesAllowHeader() throws Exception {
    mvc.perform(post("/test/get-only"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", "GET"));
}
```

Add a custom exception annotated with `@ResponseStatus(HttpStatus.NOT_FOUND)` and an actual GET-only mapping.

- [x] **Step 2: Run the MVC class to verify the tests fail**

Run: `mvn -pl sherlock-framework-web/web-spring-boot-starter -am -Dtest=FrameworkErrorHandlerMvcTest test`

Expected: annotated exception is 500 and/or the `Allow` header is absent.

- [x] **Step 3: Implement status and header propagation**

```java
private ResponseEntity<ApiResponse<Void>> failure(
        HttpStatusCode status, ErrorCode errorCode, HttpHeaders headers, HttpServletRequest request) {
    return new ResponseEntity<>(ApiResponse.failure(errorCode, traceIdResolver.resolve(request)), headers, status);
}
```

Resolve `@ResponseStatus` via Spring's annotation utility over the exception class hierarchy. For `ErrorResponse`, copy `getHeaders()` into a new `HttpHeaders` before returning the API response.

- [x] **Step 4: Re-run the MVC class to verify it passes**

Run the command from Step 2. Expected: PASS.

### Task 3: Verify module, reactor, and commit the scoped change

**Files:**
- Verify: `sherlock-framework-web/web-core/src/main/java/com/sherlock/framework/web/error/FrameworkErrorHandler.java`
- Verify: `sherlock-framework-web/web-spring-boot-starter/src/test/java/com/sherlock/framework/web/FrameworkErrorHandlerMvcTest.java`

- [x] **Step 1: Run the web reactor tests**

Run: `mvn -pl sherlock-framework-web -am test`

- [x] **Step 2: Run the repository verification build**

Run: `mvn verify`

- [x] **Step 3: Inspect the final diff and commit the handler, MVC test, and implementation plan**

Run: `git diff --check` and `git status --short`, then commit with `fix(web): preserve HTTP error semantics`.

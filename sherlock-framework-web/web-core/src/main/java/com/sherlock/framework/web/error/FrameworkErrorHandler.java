package com.sherlock.framework.web.error;

import com.sherlock.framework.base.error.BusinessException;
import com.sherlock.framework.base.error.ErrorCode;
import com.sherlock.framework.base.web.ApiResponse;
import com.sherlock.framework.web.trace.TraceIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class FrameworkErrorHandler {

    private static final ErrorCode UNAUTHORIZED = new ErrorCode("AUTH-401-001", "Authentication required");
    private static final ErrorCode FORBIDDEN = new ErrorCode("AUTH-403-001", "Access denied");
    private static final ErrorCode SYSTEM_ERROR = new ErrorCode("SYS-500-001", "System error");

    private final TraceIdResolver traceIdResolver;

    public FrameworkErrorHandler(TraceIdResolver traceIdResolver) {
        this.traceIdResolver = traceIdResolver;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception, HttpServletRequest request) {
        return failure(HttpStatus.BAD_REQUEST, exception.errorCode(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException exception, HttpServletRequest request) {
        return failure(HttpStatus.FORBIDDEN, FORBIDDEN, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException exception, HttpServletRequest request) {
        return failure(HttpStatus.UNAUTHORIZED, UNAUTHORIZED, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception exception, HttpServletRequest request) {
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, SYSTEM_ERROR, request);
    }

    private ResponseEntity<ApiResponse<Void>> failure(HttpStatus status, ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiResponse.failure(errorCode, traceIdResolver.resolve(request)));
    }
}

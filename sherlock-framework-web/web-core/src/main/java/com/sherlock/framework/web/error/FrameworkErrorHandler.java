package com.sherlock.framework.web.error;

import com.sherlock.framework.base.error.BusinessException;
import com.sherlock.framework.base.error.ErrorCode;
import com.sherlock.framework.base.web.ApiResponse;
import com.sherlock.framework.web.trace.TraceIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class FrameworkErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkErrorHandler.class);
    private static final ErrorCode UNAUTHORIZED = new ErrorCode("AUTH-401-001", "Authentication required");
    private static final ErrorCode FORBIDDEN = new ErrorCode("AUTH-403-001", "Access denied");
    private static final ErrorCode SYSTEM_ERROR = new ErrorCode("SYS-500-001", "System error");
    private static final ErrorCode MALFORMED_REQUEST = new ErrorCode("WEB-400-001", "Malformed request body");
    private static final ErrorCode INVALID_REQUEST = new ErrorCode("WEB-400-002", "Request validation failed");

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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(
            ResponseStatusException exception, HttpServletRequest request) {
        HttpStatusCode status = exception.getStatusCode();
        return failure(status, httpErrorCode(status), copyHeaders(exception.getHeaders()), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return failure(HttpStatus.BAD_REQUEST, MALFORMED_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldValidationError>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldValidationError)
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, INVALID_REQUEST.value(), INVALID_REQUEST.defaultMessage(), errors,
                        traceIdResolver.resolve(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception exception, HttpServletRequest request) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return failure(status, httpErrorCode(status), copyHeaders(errorResponse.getHeaders()), request);
        }
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(
                exception.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            HttpStatusCode status = responseStatus.code();
            return failure(status, httpErrorCode(status), request);
        }
        String traceId = traceIdResolver.resolve(request);
        LOGGER.error("Unhandled request failure; traceId={}", traceId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(SYSTEM_ERROR, traceId));
    }

    private FieldValidationError toFieldValidationError(FieldError fieldError) {
        return new FieldValidationError(fieldError.getField(), "Invalid value");
    }

    private ErrorCode httpErrorCode(HttpStatusCode status) {
        return new ErrorCode("HTTP-" + status.value() + "-001", messageFor(status));
    }

    private String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Invalid request parameters";
            case 404 -> "Resource not found";
            default -> "Request could not be processed";
        };
    }

    private HttpHeaders copyHeaders(HttpHeaders source) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(source);
        return headers;
    }

    private ResponseEntity<ApiResponse<Void>> failure(
            HttpStatusCode status, ErrorCode errorCode, HttpServletRequest request) {
        return failure(status, errorCode, HttpHeaders.EMPTY, request);
    }

    private ResponseEntity<ApiResponse<Void>> failure(
            HttpStatusCode status, ErrorCode errorCode, HttpHeaders headers, HttpServletRequest request) {
        return new ResponseEntity<>(ApiResponse.failure(errorCode, traceIdResolver.resolve(request)), headers, status);
    }

    public record FieldValidationError(String field, String message) {
    }
}

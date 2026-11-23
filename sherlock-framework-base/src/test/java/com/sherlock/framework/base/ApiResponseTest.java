package com.sherlock.framework.base;

import com.sherlock.framework.base.error.BusinessException;
import com.sherlock.framework.base.error.ErrorCode;
import com.sherlock.framework.base.web.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ApiResponseTest {

    @Test
    void failureResponseKeepsStableCodeAndTraceId() {
        var code = new ErrorCode("AUTH-401-001", "认证已失效");
        var response = ApiResponse.failure(code, "trace-123");

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("AUTH-401-001");
        assertThat(response.message()).isEqualTo("认证已失效");
        assertThat(response.data()).isNull();
        assertThat(response.traceId()).isEqualTo("trace-123");
    }

    @Test
    void successResponseUsesOkContract() {
        var response = ApiResponse.success("payload", "trace-456");

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.message()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.traceId()).isEqualTo("trace-456");
    }

    @Test
    void errorCodeRejectsBlankValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ErrorCode("  ", "message"));
    }

    @Test
    void businessExceptionUsesErrorCodeDefaultMessage() {
        var code = new ErrorCode("ORDER-400-001", "订单状态错误");
        var exception = new BusinessException(code);

        assertThat(exception.errorCode()).isSameAs(code);
        assertThat(exception).hasMessage("订单状态错误");
    }
}

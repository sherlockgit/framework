package com.sherlock.framework.web;

import com.sherlock.framework.web.trace.TraceIdFilter;
import com.sherlock.framework.web.trace.TraceIdResolver;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter(new TraceIdResolver());

    @Test
    void validRequestTraceIdIsAvailableInMdcAndResponseThenRemoved() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY)).isEqualTo("trace-123"));

        assertThat(request.getAttribute(TraceIdResolver.TRACE_ID_ATTRIBUTE)).isEqualTo("trace-123");
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-123");
        assertThat(MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY)).isNull();
    }

    @Test
    void invalidTraceIdIsGeneratedAndMdcIsRemovedWhenTheChainFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "bad trace id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, new FailingFilterChain()))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        String traceId = response.getHeader("X-Trace-Id");
        assertThat(traceId).matches("[A-Za-z0-9-]{1,64}").isNotEqualTo("bad trace id");
        assertThat(request.getAttribute(TraceIdResolver.TRACE_ID_ATTRIBUTE)).isEqualTo(traceId);
        assertThat(MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY)).isNull();
    }

    private static final class FailingFilterChain implements jakarta.servlet.FilterChain {

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                throws IOException, ServletException {
            assertThat(MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY)).isNotBlank();
            throw new ServletException(new IllegalStateException("failure"));
        }
    }
}

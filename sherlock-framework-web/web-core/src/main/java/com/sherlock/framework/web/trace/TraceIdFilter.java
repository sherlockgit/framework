package com.sherlock.framework.web.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TraceIdFilter extends OncePerRequestFilter implements Ordered {

    public static final String MDC_TRACE_ID_KEY = "traceId";

    private final TraceIdResolver traceIdResolver;

    public TraceIdFilter(TraceIdResolver traceIdResolver) {
        this.traceIdResolver = traceIdResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = traceIdResolver.resolve(request);
        request.setAttribute(TraceIdResolver.TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(TraceIdResolver.TRACE_ID_HEADER, traceId);
        MDC.put(MDC_TRACE_ID_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

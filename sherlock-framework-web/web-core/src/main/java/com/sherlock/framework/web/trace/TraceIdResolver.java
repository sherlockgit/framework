package com.sherlock.framework.web.trace;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;
import java.util.regex.Pattern;

public class TraceIdResolver {

    public static final String TRACE_ID_ATTRIBUTE = TraceIdResolver.class.getName() + ".TRACE_ID";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9-]{1,64}");

    public String resolve(HttpServletRequest request) {
        Object requestTraceId = request.getAttribute(TRACE_ID_ATTRIBUTE);
        if (requestTraceId instanceof String traceId && isValid(traceId)) {
            return traceId;
        }
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (isValid(traceId)) {
            return traceId;
        }
        return UUID.randomUUID().toString();
    }

    public boolean isValid(String traceId) {
        return traceId != null && TRACE_ID_PATTERN.matcher(traceId).matches();
    }
}

package com.sherlock.framework.web.trace;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;
import java.util.regex.Pattern;

public class TraceIdResolver {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9-]{1,64}");

    public String resolve(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId != null && TRACE_ID_PATTERN.matcher(traceId).matches()) {
            return traceId;
        }
        return UUID.randomUUID().toString();
    }
}

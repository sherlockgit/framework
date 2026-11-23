package com.sherlock.framework.data.web;

import com.sherlock.framework.data.scope.DataScopeContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.core.Ordered;

import java.io.IOException;

public class DataScopeContextCleanupFilter implements Filter, Ordered {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        DataScopeContext.clear();
        try {
            filterChain.doFilter(request, response);
        } finally {
            DataScopeContext.clear();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

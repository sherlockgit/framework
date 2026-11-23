package com.sherlock.framework.web.autoconfigure;

import com.sherlock.framework.web.error.FrameworkErrorHandler;
import com.sherlock.framework.web.trace.TraceIdFilter;
import com.sherlock.framework.web.trace.TraceIdResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TraceIdResolver traceIdResolver() {
        return new TraceIdResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    TraceIdFilter traceIdFilter(TraceIdResolver traceIdResolver) {
        return new TraceIdFilter(traceIdResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    FrameworkErrorHandler frameworkErrorHandler(TraceIdResolver traceIdResolver) {
        return new FrameworkErrorHandler(traceIdResolver);
    }
}

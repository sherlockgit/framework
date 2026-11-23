package com.sherlock.framework.tenant.autoconfigure;

import com.sherlock.framework.tenant.web.TenantContextCleanupFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TenantContextCleanupFilter tenantContextCleanupFilter() {
        return new TenantContextCleanupFilter();
    }
}

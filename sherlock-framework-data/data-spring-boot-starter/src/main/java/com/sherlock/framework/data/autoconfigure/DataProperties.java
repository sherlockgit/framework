package com.sherlock.framework.data.autoconfigure;

import com.sherlock.framework.data.mybatis.TenantTablePolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties("sherlock.data")
public class DataProperties {

    private Set<String> globalTables = TenantTablePolicy.PLATFORM_GLOBAL_TABLES;
    private Set<String> tenantTables = Set.of();

    public Set<String> getGlobalTables() {
        return globalTables;
    }

    public void setGlobalTables(Set<String> globalTables) {
        this.globalTables = Set.copyOf(globalTables);
    }

    public Set<String> getTenantTables() {
        return tenantTables;
    }

    public void setTenantTables(Set<String> tenantTables) {
        this.tenantTables = Set.copyOf(tenantTables);
    }
}

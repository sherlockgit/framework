package com.sherlock.framework.cache.redis;

import com.sherlock.framework.base.error.BusinessException;
import com.sherlock.framework.tenant.TenantContext;
import com.sherlock.framework.tenant.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantCacheKeyFactoryTest {

    private final TenantCacheKeyFactory factory = new TenantCacheKeyFactory();

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void keyAlwaysIncludesCurrentTenant() {
        TenantContext.set(new TenantId(42L));

        assertThat(factory.create("order", "1001").value()).isEqualTo("order:42:1001");
    }

    @Test
    void missingTenantRejectsCacheAccess() {
        assertThatThrownBy(() -> factory.create("order", "1001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant context");
    }
}

package com.sherlock.framework.tenant;

import com.sherlock.framework.base.error.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantIdMustBePositive() {
        assertThatThrownBy(() -> new TenantId(0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TenantId(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireWithoutTenantUsesStableTenantErrorCode() {
        assertThatThrownBy(TenantContext::require)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode().value())
                .isEqualTo("TENANT-400-001");
    }

    @Test
    void executeWithRestoresPreviousTenant() {
        TenantContext.set(new TenantId(10L));

        TenantContext.executeWith(new TenantId(20L), () ->
                assertThat(TenantContext.require().value()).isEqualTo(20L));

        assertThat(TenantContext.require().value()).isEqualTo(10L);
    }

    @Test
    void executeWithRestoresPreviousTenantWhenActionThrows() {
        TenantContext.set(new TenantId(10L));

        assertThatThrownBy(() -> TenantContext.executeWith(new TenantId(20L), () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(TenantContext.require().value()).isEqualTo(10L);
    }

    @Test
    void snapshotRestoresCapturedTenant() {
        TenantContext.set(new TenantId(10L));
        TenantContextSnapshot snapshot = TenantContextSnapshot.capture();
        TenantContext.set(new TenantId(20L));

        snapshot.restore();

        assertThat(TenantContext.require().value()).isEqualTo(10L);
    }
}

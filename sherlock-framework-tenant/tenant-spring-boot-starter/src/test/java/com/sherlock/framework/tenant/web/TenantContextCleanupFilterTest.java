package com.sherlock.framework.tenant.web;

import com.sherlock.framework.tenant.TenantContext;
import com.sherlock.framework.tenant.TenantId;
import com.sherlock.framework.tenant.autoconfigure.TenantAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextCleanupFilterTest {

    private final TenantContextCleanupFilter filter = new TenantContextCleanupFilter();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void filterClearsContextWhenChainThrows() {
        assertThatThrownBy(() -> filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (request, response) -> {
                    TenantContext.set(new TenantId(1L));
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    void filterClearsExistingContextBeforeInvokingChain() throws Exception {
        TenantContext.set(new TenantId(99L));

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (request, response) -> assertThat(TenantContext.current()).isEmpty());

        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    void autoConfigurationRegistersFilterOnlyForServletWebApplications() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(TenantContextCleanupFilter.class));

        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class))
                .run(context -> assertThat(context).hasSingleBean(TenantContextCleanupFilter.class));
    }

    @Test
    void cleanupFilterRunsBeforeOtherFilters() {
        assertThat(filter).isInstanceOf(Ordered.class);
        assertThat(((Ordered) filter).getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}

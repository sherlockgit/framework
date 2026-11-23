package com.sherlock.framework.data.web;

import com.sherlock.framework.data.scope.DataScope;
import com.sherlock.framework.data.scope.DataScopeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataScopeContextCleanupFilterTest {

    private final DataScopeContextCleanupFilter filter = new DataScopeContextCleanupFilter();

    @AfterEach
    void clearContext() {
        DataScopeContext.clear();
    }

    @Test
    void filterClearsStaleScopeBeforeAndAfterTheRequest() throws Exception {
        DataScopeContext.set(DataScope.allTenant());

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            assertThat(DataScopeContext.current()).isEmpty();
            DataScopeContext.set(DataScope.self());
        });

        assertThat(DataScopeContext.current()).isEmpty();
    }

    @Test
    void filterClearsScopeWhenTheChainThrows() {
        assertThatThrownBy(() -> filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (request, response) -> {
                    DataScopeContext.set(DataScope.self());
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(DataScopeContext.current()).isEmpty();
    }

    @Test
    void filterClearsScopeWhenAnInnerFilterShortCircuits() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            DataScopeContext.set(DataScope.self());
            // Simulates an inner security filter writing a response and returning without a controller call.
        });

        assertThat(DataScopeContext.current()).isEmpty();
    }

    @Test
    void cleanupFilterRunsBeforeSecurityAndDataBindingFilters() {
        assertThat(filter).isInstanceOf(Ordered.class);
        assertThat(((Ordered) filter).getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}

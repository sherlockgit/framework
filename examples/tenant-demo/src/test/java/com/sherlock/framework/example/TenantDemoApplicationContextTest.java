package com.sherlock.framework.example;

import com.sherlock.framework.cache.CachePort;
import com.sherlock.framework.data.scope.DataScope;
import com.sherlock.framework.data.scope.DataScopeContext;
import com.sherlock.framework.example.order.OrderMapper;
import com.sherlock.framework.security.AuthenticatedPrincipal;
import com.sherlock.framework.security.PrincipalContext;
import com.sherlock.framework.tenant.TenantContext;
import com.sherlock.framework.tenant.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = TenantDemoApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:tenant_demo_context;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "sherlock.security.jwt.issuer=tenant-demo-context",
                "sherlock.security.jwt.audience=tenant-demo-context-api"
        })
@Import(TenantDemoApplicationContextTest.TestBeans.class)
class TenantDemoApplicationContextTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private OrderMapper orderMapper;

    @AfterEach
    void clearContexts() {
        DataScopeContext.clear();
        PrincipalContext.clear();
        TenantContext.clear();
    }

    @Test
    void applicationContextLoadsWithoutDocker() {
        assertThat(context.getBean(CachePort.class)).isNotNull();
    }

    @Test
    void myBatisInjectsTenantIdForColumnListedInsertAndKeepsSameBusinessKeyIsolated() {
        bind(1L, 101L);
        orderMapper.insertOrder("context-shared-key", 101L, 10L, "tenant-one");
        assertThat(orderMapper.selectByBusinessKey("context-shared-key").getTenantId()).isEqualTo(1L);
        assertThat(orderMapper.selectByBusinessKey("context-shared-key").getName()).isEqualTo("tenant-one");

        bind(2L, 201L);
        orderMapper.insertOrder("context-shared-key", 201L, 20L, "tenant-two");
        assertThat(orderMapper.selectByBusinessKey("context-shared-key").getTenantId()).isEqualTo(2L);
        assertThat(orderMapper.selectByBusinessKey("context-shared-key").getName()).isEqualTo("tenant-two");
    }

    private static void bind(long tenantId, long userId) {
        TenantId tenant = new TenantId(tenantId);
        TenantContext.set(tenant);
        PrincipalContext.set(new AuthenticatedPrincipal(userId, tenant, Set.of("order:read"), 1L));
        DataScopeContext.set(DataScope.allTenant());
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        CachePort cachePort() {
            return mock(CachePort.class);
        }
    }
}

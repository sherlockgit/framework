package com.sherlock.framework.data.autoconfigure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DataAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataAutoConfiguration.class));

    @Test
    void registersTenantAndDataPermissionInterceptors() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
            assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors()).hasSize(2);
        });
    }

    @Test
    void addsIsolationInterceptorsToAnExistingMybatisPlusInterceptor() {
        contextRunner.withBean(MybatisPlusInterceptor.class, MybatisPlusInterceptor::new)
                .run(context -> assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors()).hasSize(2));
    }

    @Test
    void declaredTenantTableWithoutTenantIdMappingFailsAtStartup() {
        contextRunner.withPropertyValues("sherlock.data.tenant-tables=orders")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("orders")
                        .hasMessageContaining("tenant_id"));
    }

    @Test
    void mappedTenantTableWithoutTenantIdColumnFailsAtStartup() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "orders-mapping");
        assistant.setCurrentNamespace("orders-mapping");
        TableInfoHelper.initTableInfo(assistant, OrderWithoutTenantId.class);

        try {
            contextRunner.withPropertyValues("sherlock.data.tenant-tables=orders")
                    .run(context -> assertThat(context).hasFailed()
                            .getFailure().hasMessageContaining("orders")
                            .hasMessageContaining("tenant_id"));
        } finally {
            TableInfoHelper.remove(OrderWithoutTenantId.class);
        }
    }

    @TableName("orders")
    static class OrderWithoutTenantId {
        @TableId
        private Long id;
        private String name;
    }
}

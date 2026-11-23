package com.sherlock.framework.data.autoconfigure;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.sherlock.framework.data.mybatis.FrameworkDataPermissionHandler;
import com.sherlock.framework.data.mybatis.FrameworkTenantLineHandler;
import com.sherlock.framework.data.mybatis.TenantTablePolicy;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(DataProperties.class)
public class DataAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TenantTablePolicy tenantTablePolicy(DataProperties properties) {
        return new TenantTablePolicy(properties.getGlobalTables(), properties.getTenantTables());
    }

    @Bean
    @ConditionalOnMissingBean
    MybatisPlusInterceptor frameworkMybatisPlusInterceptor() {
        return new MybatisPlusInterceptor();
    }

    @Bean
    SmartInitializingSingleton frameworkMybatisIsolationConfigurer(
            ObjectProvider<MybatisPlusInterceptor> interceptors, TenantTablePolicy tablePolicy) {
        return () -> interceptors.orderedStream()
                .forEach(interceptor -> addIsolationInterceptors(interceptor, tablePolicy));
    }

    @Bean
    SmartInitializingSingleton tenantTableMappingValidator(TenantTablePolicy tablePolicy) {
        return () -> tablePolicy.validateMappedTenantTables(TableInfoHelper.getTableInfos());
    }

    private static void addIsolationInterceptors(MybatisPlusInterceptor interceptor, TenantTablePolicy tablePolicy) {
        if (interceptor.getInterceptors().stream()
                .filter(TenantLineInnerInterceptor.class::isInstance)
                .map(TenantLineInnerInterceptor.class::cast)
                .noneMatch(inner -> inner.getTenantLineHandler() instanceof FrameworkTenantLineHandler)) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new FrameworkTenantLineHandler(tablePolicy)));
        }
        if (interceptor.getInterceptors().stream()
                .filter(DataPermissionInterceptor.class::isInstance)
                .map(DataPermissionInterceptor.class::cast)
                .noneMatch(inner -> inner.getDataPermissionHandler() instanceof FrameworkDataPermissionHandler)) {
            interceptor.addInnerInterceptor(new DataPermissionInterceptor(new FrameworkDataPermissionHandler(tablePolicy)));
        }
    }
}

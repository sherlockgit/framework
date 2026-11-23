package com.sherlock.framework.data.autoconfigure;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.sherlock.framework.data.mybatis.FrameworkDataPermissionHandler;
import com.sherlock.framework.data.mybatis.FrameworkTenantLineInnerInterceptor;
import com.sherlock.framework.data.mybatis.TenantTablePolicy;
import com.sherlock.framework.data.web.DataScopeContextCleanupFilter;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

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
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    DataScopeContextCleanupFilter dataScopeContextCleanupFilter() {
        return new DataScopeContextCleanupFilter();
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
        List<InnerInterceptor> innerInterceptors = new ArrayList<>(interceptor.getInterceptors());
        List<TenantLineInnerInterceptor> tenantInterceptors = innerInterceptors.stream()
                .filter(TenantLineInnerInterceptor.class::isInstance)
                .map(TenantLineInnerInterceptor.class::cast)
                .toList();
        if (tenantInterceptors.size() > 1) {
            throw new IllegalStateException(
                    "A MybatisPlusInterceptor must contain exactly one TenantLineInnerInterceptor");
        }
        if (!tenantInterceptors.isEmpty()
                && !(tenantInterceptors.getFirst() instanceof FrameworkTenantLineInnerInterceptor)) {
            throw new IllegalStateException(
                    "A MybatisPlusInterceptor already contains a TenantLineInnerInterceptor that is not framework-managed");
        }

        List<DataPermissionInterceptor> dataPermissionInterceptors = innerInterceptors.stream()
                .filter(DataPermissionInterceptor.class::isInstance)
                .map(DataPermissionInterceptor.class::cast)
                .toList();
        if (dataPermissionInterceptors.size() > 1) {
            throw new IllegalStateException(
                    "A MybatisPlusInterceptor must contain exactly one DataPermissionInterceptor");
        }
        if (!dataPermissionInterceptors.isEmpty()
                && !(dataPermissionInterceptors.getFirst().getDataPermissionHandler()
                instanceof FrameworkDataPermissionHandler)) {
            throw new IllegalStateException(
                    "A MybatisPlusInterceptor already contains a DataPermissionInterceptor that is not framework-managed");
        }

        List<InnerInterceptor> paginationInterceptors = innerInterceptors.stream()
                .filter(PaginationInnerInterceptor.class::isInstance)
                .toList();
        List<InnerInterceptor> orderedInterceptors = innerInterceptors.stream()
                .filter(inner -> !(inner instanceof TenantLineInnerInterceptor)
                        && !(inner instanceof DataPermissionInterceptor)
                        && !(inner instanceof PaginationInnerInterceptor))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        orderedInterceptors.add(tenantInterceptors.isEmpty()
                ? new FrameworkTenantLineInnerInterceptor(tablePolicy)
                : tenantInterceptors.getFirst());
        orderedInterceptors.add(dataPermissionInterceptors.isEmpty()
                ? new DataPermissionInterceptor(new FrameworkDataPermissionHandler(tablePolicy))
                : dataPermissionInterceptors.getFirst());
        orderedInterceptors.addAll(paginationInterceptors);
        interceptor.setInterceptors(orderedInterceptors);
    }
}

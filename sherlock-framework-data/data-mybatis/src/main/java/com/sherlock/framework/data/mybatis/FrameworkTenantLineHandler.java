package com.sherlock.framework.data.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.sherlock.framework.tenant.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Objects;

public final class FrameworkTenantLineHandler implements TenantLineHandler {

    public static final String TENANT_ID_COLUMN = "tenant_id";

    private final TenantTablePolicy tablePolicy;

    public FrameworkTenantLineHandler(TenantTablePolicy tablePolicy) {
        this.tablePolicy = Objects.requireNonNull(tablePolicy, "tablePolicy must not be null");
    }

    @Override
    public Expression getTenantId() {
        return new LongValue(TenantContext.require().value());
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_ID_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return !tablePolicy.isTenantTable(tableName);
    }
}

package com.sherlock.framework.data.mybatis;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.sherlock.framework.base.error.BusinessException;
import com.sherlock.framework.base.error.ErrorCode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;

import java.util.Collection;

/**
 * Keeps tenant ownership immutable for framework-managed tenant tables.
 */
public final class FrameworkTenantLineInnerInterceptor extends TenantLineInnerInterceptor {

    private static final String EXPLICIT_TENANT_WRITE_MESSAGE =
            "Explicit tenant_id writes are forbidden for tenant tables";
    private static final ErrorCode MISSING_INSERT_COLUMN_LIST = new ErrorCode(
            "DATA-400-001", "Tenant table inserts must declare target columns");

    private final TenantTablePolicy tablePolicy;

    public FrameworkTenantLineInnerInterceptor(TenantTablePolicy tablePolicy) {
        super(new FrameworkTenantLineHandler(tablePolicy));
        this.tablePolicy = tablePolicy;
    }

    @Override
    protected void processInsert(Insert insert, int index, String sql, Object object) {
        if (isManagedTenantTable(insert.getTable().getName())) {
            if (omitsTargetColumns(insert)) {
                throw new BusinessException(MISSING_INSERT_COLUMN_LIST);
            }
            if (containsTenantColumn(insert.getColumns())
                    || containsTenantColumn(insert.getSetUpdateSets())
                    || containsTenantColumn(insert.getDuplicateUpdateSets())) {
                throw new IllegalStateException(EXPLICIT_TENANT_WRITE_MESSAGE);
            }
        }
        super.processInsert(insert, index, sql, object);
    }

    @Override
    protected void processUpdate(Update update, int index, String sql, Object object) {
        if (isManagedTenantTable(update.getTable().getName())
                && containsTenantColumn(update.getUpdateSets())) {
            throw new IllegalStateException(EXPLICIT_TENANT_WRITE_MESSAGE);
        }
        super.processUpdate(update, index, sql, object);
    }

    private boolean isManagedTenantTable(String tableName) {
        return tablePolicy.isTenantTable(tableName);
    }

    public TenantTablePolicy tablePolicy() {
        return tablePolicy;
    }

    private boolean omitsTargetColumns(Insert insert) {
        return insert.getColumns() == null || insert.getColumns().isEmpty();
    }

    private boolean containsTenantColumn(Iterable<Column> columns) {
        if (columns == null) {
            return false;
        }
        for (Column column : columns) {
            if (FrameworkTenantLineHandler.TENANT_ID_COLUMN.equalsIgnoreCase(column.getColumnName())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTenantColumn(Collection<UpdateSet> updateSets) {
        return updateSets != null && updateSets.stream()
                .map(UpdateSet::getColumns)
                .anyMatch(this::containsTenantColumn);
    }
}

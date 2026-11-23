package com.sherlock.framework.data.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.sherlock.framework.data.scope.DataScope;
import com.sherlock.framework.data.scope.DataScopeContext;
import com.sherlock.framework.security.PrincipalContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.Objects;

public final class FrameworkDataPermissionHandler implements MultiDataPermissionHandler {

    private final TenantTablePolicy tablePolicy;

    public FrameworkDataPermissionHandler(TenantTablePolicy tablePolicy) {
        this.tablePolicy = Objects.requireNonNull(tablePolicy, "tablePolicy must not be null");
    }

    public TenantTablePolicy tablePolicy() {
        return tablePolicy;
    }

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        return dataScopeExpression(null);
    }

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        if (table != null && !tablePolicy.isTenantTable(table.getName())) {
            return null;
        }
        return dataScopeExpression(table);
    }

    private Expression dataScopeExpression(Table table) {
        DataScope scope = DataScopeContext.require();
        if (scope.isAllTenant()) {
            return null;
        }

        long userId = PrincipalContext.require().userId();
        Expression selfExpression = scope.includesSelf()
                ? new EqualsTo(column(table, "creator_id"), new LongValue(userId))
                : denyExpression();
        Expression departmentExpression = scope.visibleDepartmentIds().isEmpty()
                ? denyExpression()
                : new InExpression(
                        column(table, "department_id"),
                        new ParenthesedExpressionList<>(scope.visibleDepartmentIds().stream().sorted().map(LongValue::new).toList()));
        return new OrExpression(selfExpression, departmentExpression);
    }

    private static Column column(Table table, String name) {
        return table == null ? new Column(name) : new Column(table, name);
    }

    private static Expression denyExpression() {
        return new EqualsTo(new LongValue(1L), new LongValue(0L));
    }
}

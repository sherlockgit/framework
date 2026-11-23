package com.sherlock.framework.data.scope;

import com.sherlock.framework.security.AuthenticatedPrincipal;
import com.sherlock.framework.security.PrincipalContext;

import java.util.Objects;
import java.util.function.Function;

public final class DataScopeResolver {

    private final Function<AuthenticatedPrincipal, DataScope> policySnapshot;

    public DataScopeResolver(Function<AuthenticatedPrincipal, DataScope> policySnapshot) {
        this.policySnapshot = Objects.requireNonNull(policySnapshot, "policySnapshot must not be null");
    }

    public DataScope resolve() {
        DataScope scope = policySnapshot.apply(PrincipalContext.require());
        return Objects.requireNonNull(scope, "policySnapshot must return a data scope");
    }

    public DataScope resolveAndBind() {
        DataScope scope = resolve();
        DataScopeContext.set(scope);
        return scope;
    }
}

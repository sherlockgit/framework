package com.sherlock.framework.cache.redis;

import com.sherlock.framework.cache.CacheKey;
import com.sherlock.framework.tenant.TenantContext;

public final class TenantCacheKeyFactory {

    public CacheKey create(String namespace, String resource) {
        return new CacheKey(namespace, Long.toString(TenantContext.require().value()), resource);
    }
}

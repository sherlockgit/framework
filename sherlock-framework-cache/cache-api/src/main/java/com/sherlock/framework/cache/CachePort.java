package com.sherlock.framework.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Tenant-aware cache operations using UTF-8 string values rather than Java object serialization.
 */
public interface CachePort {

    Optional<String> get(String namespace, String resource);

    void put(String namespace, String resource, String value, Duration ttl);

    void evict(String namespace, String resource);
}

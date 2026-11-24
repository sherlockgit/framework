package com.sherlock.framework.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * 面向租户的缓存操作，使用 UTF-8 字符串值，避免 Java 对象序列化。
 */
public interface CachePort {

    Optional<String> get(String namespace, String resource);

    void put(String namespace, String resource, String value, Duration ttl);

    void evict(String namespace, String resource);
}

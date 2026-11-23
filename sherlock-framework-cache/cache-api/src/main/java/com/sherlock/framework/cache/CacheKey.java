package com.sherlock.framework.cache;

import java.util.Objects;

/**
 * A structured cache key whose individual segments are safe to join with a colon.
 */
public record CacheKey(String namespace, String tenantId, String resource) {

    public CacheKey {
        validateSegment(namespace, "namespace");
        validateSegment(tenantId, "tenantId");
        validateSegment(resource, "resource");
    }

    public String value() {
        return namespace + ":" + tenantId + ":" + resource;
    }

    private static void validateSegment(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank() || value.indexOf(':') >= 0 || value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(name + " must not be empty or contain colons or whitespace");
        }
    }
}

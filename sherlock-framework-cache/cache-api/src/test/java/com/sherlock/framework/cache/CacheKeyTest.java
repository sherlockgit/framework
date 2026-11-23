package com.sherlock.framework.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheKeyTest {

    @Test
    void rejectsEmptyNamespace() {
        assertThatThrownBy(() -> new CacheKey("", "42", "order"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsColonInResource() {
        assertThatThrownBy(() -> new CacheKey("order", "42", "item:1001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWhitespaceInSegments() {
        assertThatThrownBy(() -> new CacheKey("order", "4 2", "1001"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

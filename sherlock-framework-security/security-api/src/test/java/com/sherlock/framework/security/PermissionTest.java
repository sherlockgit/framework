package com.sherlock.framework.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionTest {

    @Test
    void permissionValueMustContainText() {
        assertThat(new Permission("order:read").value()).isEqualTo("order:read");
        assertThatThrownBy(() -> new Permission(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

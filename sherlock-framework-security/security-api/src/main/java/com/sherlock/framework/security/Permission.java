package com.sherlock.framework.security;

public record Permission(String value) {

    public Permission {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("permission value must not be blank");
        }
    }
}

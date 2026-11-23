package com.sherlock.framework.base.error;

public record ErrorCode(String value, String defaultMessage) {

    public ErrorCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Error code value must not be blank");
        }
        if (defaultMessage == null || defaultMessage.isBlank()) {
            throw new IllegalArgumentException("Error code default message must not be blank");
        }
    }
}

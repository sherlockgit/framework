package com.sherlock.framework.base.page;

public record PageRequest(int pageNumber, int pageSize) {

    public PageRequest {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be at least 1");
        }
        if (pageSize < 1 || pageSize > 500) {
            throw new IllegalArgumentException("Page size must be between 1 and 500");
        }
    }
}

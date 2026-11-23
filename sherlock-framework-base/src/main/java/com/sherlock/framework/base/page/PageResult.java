package com.sherlock.framework.base.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record PageResult<T>(List<T> records, long total, int pageNumber, int pageSize) {

    public PageResult {
        records = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(records, "records must not be null")));
        if (total < 0) {
            throw new IllegalArgumentException("Total must not be negative");
        }
        new PageRequest(pageNumber, pageSize);
    }
}

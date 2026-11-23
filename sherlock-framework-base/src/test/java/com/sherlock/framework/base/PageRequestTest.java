package com.sherlock.framework.base;

import com.sherlock.framework.base.page.PageRequest;
import com.sherlock.framework.base.page.PageResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    @Test
    void pageRequestRejectsNonPositivePageNumber() {
        assertThatThrownBy(() -> new PageRequest(0, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pageRequestRejectsPageSizeGreaterThanFiveHundred() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PageRequest(1, 501));
    }

    @Test
    void pageResultDefensivelyCopiesRecords() {
        var source = new ArrayList<>(List.of("first"));
        var result = new PageResult<>(source, 1, 1, 20);
        source.add("second");

        assertThat(result.records()).containsExactly("first");
        assertThatThrownBy(() -> result.records().add("third"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void pageResultRejectsNegativeTotal() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PageResult<>(List.of(), -1, 1, 20));
    }
}

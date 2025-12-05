package com.corems.common.utils.db;

import com.corems.common.utils.db.utils.PaginationUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationUtilTest {

    @Test
    void parseSortWithValidInput() {
        Sort sort = PaginationUtil.parseSort("name:asc,createdAt:desc", List.of("name", "createdAt"));
        assertThat(sort.isSorted()).isTrue();
        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(sort.getOrderFor("createdAt")).isNotNull();
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void parseSortWithEmptyString() {
        Sort sort = PaginationUtil.parseSort("", List.of("name"));
        assertThat(sort.isUnsorted()).isTrue();
    }

    @Test
    void parseSortWithBlankString() {
        Sort sort = PaginationUtil.parseSort("   ", List.of("name"));
        assertThat(sort.isUnsorted()).isTrue();
    }

    @Test
    void parseSortWithMalformedInput_leadingColon() {
        Sort sort = PaginationUtil.parseSort(":asc", List.of("name"));
        assertThat(sort.isUnsorted()).isTrue();
    }

    @Test
    void parseSortWithMalformedInput_trailingColon() {
        Sort sort = PaginationUtil.parseSort("name:", List.of("name"));
        assertThat(sort.isSorted()).isTrue();
        assertThat(sort.getOrderFor("name")).isNotNull();
    }

    @Test
    void parseSortWithMalformedInput_multipleColons() {
        Sort sort = PaginationUtil.parseSort("name::asc", List.of("name"));
        assertThat(sort.isSorted()).isTrue();
    }

    @Test
    void parseSortWithMalformedInput_emptyParts() {
        Sort sort = PaginationUtil.parseSort(",,name:asc,,", List.of("name"));
        assertThat(sort.isSorted()).isTrue();
        assertThat(sort.getOrderFor("name")).isNotNull();
    }

    @Test
    void parseSortIgnoresUnallowedFields() {
        Sort sort = PaginationUtil.parseSort("name:asc,forbidden:desc", List.of("name"));
        assertThat(sort.isSorted()).isTrue();
        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("forbidden")).isNull();
    }
}


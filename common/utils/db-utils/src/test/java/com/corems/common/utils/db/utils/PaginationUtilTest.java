package com.corems.common.utils.db.utils;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PaginationUtilTest {

    @Test
    void buildPageable_WithDefaultValues_ReturnsDefaultPageable() {
        Pageable pageable = PaginationUtil.buildPageable(
            Optional.empty(), 
            Optional.empty(), 
            Optional.empty(), 
            List.of("name", "age")
        );

        assertEquals(0, pageable.getPageNumber()); // 0-based, so page 1 becomes 0
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.unsorted(), pageable.getSort());
    }

    @Test
    void buildPageable_WithCustomPageAndSize_ReturnsCustomPageable() {
        Pageable pageable = PaginationUtil.buildPageable(
            Optional.of(3), 
            Optional.of(50), 
            Optional.empty(), 
            List.of("name")
        );

        assertEquals(2, pageable.getPageNumber()); // 3rd page becomes index 2
        assertEquals(50, pageable.getPageSize());
    }

    @Test
    void buildPageable_WithPageLessThanOne_UsesDefaultPage() {
        Pageable pageable = PaginationUtil.buildPageable(
            Optional.of(0), 
            Optional.empty(), 
            Optional.empty(), 
            List.of("name")
        );

        assertEquals(0, pageable.getPageNumber()); // Default page 1 becomes index 0
    }

    @Test
    void buildPageable_WithNegativePage_UsesDefaultPage() {
        Pageable pageable = PaginationUtil.buildPageable(
            Optional.of(-5), 
            Optional.empty(), 
            Optional.empty(), 
            List.of("name")
        );

        assertEquals(0, pageable.getPageNumber()); // Default page 1 becomes index 0
    }

    @Test
    void buildPageable_WithSizeLessThanOne_UsesMinimumSize() {
        Pageable pageable = PaginationUtil.buildPageable(
            Optional.empty(), 
            Optional.of(0), 
            Optional.empty(), 
            List.of("name")
        );

        assertEquals(1, pageable.getPageSize());
    }

    @Test
    void buildPageable_WithSizeGreaterThanMax_UsesMaxSize() {
        Pageable pageable = PaginationUtil.buildPageable(
            Optional.empty(), 
            Optional.of(2000), 
            Optional.empty(), 
            List.of("name")
        );

        assertEquals(1000, pageable.getPageSize());
    }

    @Test
    void buildPageable_WithValidSort_AppliesSort() {
        List<String> allowedFields = List.of("name", "age", "email");
        
        Pageable pageable = PaginationUtil.buildPageable(
            Optional.empty(), 
            Optional.empty(), 
            Optional.of("name:asc,age:desc"), 
            allowedFields
        );

        Sort sort = pageable.getSort();
        assertFalse(sort.isUnsorted());
        
        List<Sort.Order> orders = sort.toList();
        assertEquals(2, orders.size());
        
        assertEquals("name", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());
        
        assertEquals("age", orders.get(1).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(1).getDirection());
    }

    @Test
    void parseSort_WithNullInput_ReturnsUnsorted() {
        Sort sort = PaginationUtil.parseSort(null, List.of("name", "age"));
        
        assertTrue(sort.isUnsorted());
    }

    @Test
    void parseSort_WithBlankInput_ReturnsUnsorted() {
        Sort sort = PaginationUtil.parseSort("  ", List.of("name", "age"));
        
        assertTrue(sort.isUnsorted());
    }

    @Test
    void parseSort_WithSingleFieldAscending_ReturnsAscendingSort() {
        Sort sort = PaginationUtil.parseSort("name:asc", List.of("name", "age"));
        
        assertFalse(sort.isUnsorted());
        List<Sort.Order> orders = sort.toList();
        assertEquals(1, orders.size());
        assertEquals("name", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());
    }

    @Test
    void parseSort_WithSingleFieldDescending_ReturnsDescendingSort() {
        Sort sort = PaginationUtil.parseSort("name:desc", List.of("name", "age"));
        
        assertFalse(sort.isUnsorted());
        List<Sort.Order> orders = sort.toList();
        assertEquals(1, orders.size());
        assertEquals("name", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
    }

    @Test
    void parseSort_WithFieldWithoutDirection_UsesDescendingDefault() {
        Sort sort = PaginationUtil.parseSort("name", List.of("name", "age"));
        
        assertFalse(sort.isUnsorted());
        List<Sort.Order> orders = sort.toList();
        assertEquals(1, orders.size());
        assertEquals("name", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
    }

    @Test
    void parseSort_WithMultipleFields_ReturnsMultipleOrders() {
        Sort sort = PaginationUtil.parseSort("name:asc,age:desc,email:asc", 
            List.of("name", "age", "email"));
        
        assertFalse(sort.isUnsorted());
        List<Sort.Order> orders = sort.toList();
        assertEquals(3, orders.size());
        
        assertEquals("name", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());
        
        assertEquals("age", orders.get(1).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(1).getDirection());
        
        assertEquals("email", orders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(2).getDirection());
    }

    @Test
    void parseSort_WithFieldNotInAllowedList_IgnoresField() {
        Sort sort = PaginationUtil.parseSort("name:asc,invalidField:desc,age:asc", 
            List.of("name", "age"));
        
        assertFalse(sort.isUnsorted());
        List<Sort.Order> orders = sort.toList();
        assertEquals(2, orders.size());
        
        assertEquals("name", orders.get(0).getProperty());
        assertEquals("age", orders.get(1).getProperty());
    }

    @Test
    void parseSort_WithBlankFields_IgnoresBlankFields() {
        Sort sort = PaginationUtil.parseSort("name:asc,,age:desc,  ", 
            List.of("name", "age"));
        
        assertFalse(sort.isUnsorted());
        List<Sort.Order> orders = sort.toList();
        assertEquals(2, orders.size());
        
        assertEquals("name", orders.get(0).getProperty());
        assertEquals("age", orders.get(1).getProperty());
    }

    @Test
    void parseSort_WithInvalidDirection_UsesDescendingDefault() {
        Sort sort = PaginationUtil.parseSort("name:invalid", List.of("name"));
        
        assertFalse(sort.isUnsorted());
        List<Sort.Order> orders = sort.toList();
        assertEquals(1, orders.size());
        assertEquals("name", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
    }

    @Test
    void parseSort_WithCaseInsensitiveDirection_ParsesCorrectly() {
        Sort sort = PaginationUtil.parseSort("name:ASC,age:DESC", List.of("name", "age"));
        
        assertFalse(sort.isUnsorted());
        List<Sort.Order> orders = sort.toList();
        assertEquals(2, orders.size());
        
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());
        assertEquals(Sort.Direction.DESC, orders.get(1).getDirection());
    }

    @Test
    void parseSort_WithAllFieldsNotAllowed_ReturnsUnsorted() {
        Sort sort = PaginationUtil.parseSort("invalidField1:asc,invalidField2:desc", 
            List.of("name", "age"));
        
        assertTrue(sort.isUnsorted());
    }

    @Test
    void sanitizeSearch_WithValidSearch_ReturnsTrimmmedSearch() {
        String result = PaginationUtil.sanitizeSearch(Optional.of("  search term  "));
        
        assertEquals("search term", result);
    }

    @Test
    void sanitizeSearch_WithEmptySearch_ReturnsEmptyString() {
        String result = PaginationUtil.sanitizeSearch(Optional.of(""));
        
        assertEquals("", result);
    }

    @Test
    void sanitizeSearch_WithBlankSearch_ReturnsEmptyString() {
        String result = PaginationUtil.sanitizeSearch(Optional.of("   "));
        
        assertEquals("", result);
    }

    @Test
    void sanitizeSearch_WithEmptyOptional_ReturnsEmptyString() {
        String result = PaginationUtil.sanitizeSearch(Optional.empty());
        
        assertEquals("", result);
    }

    @Test
    void sanitizeSearch_WithNullInOptional_ReturnsEmptyString() {
        String result = PaginationUtil.sanitizeSearch(Optional.ofNullable(null));
        
        assertEquals("", result);
    }

    @Test
    void buildPageable_IntegrationTest_WithAllParameters() {
        Pageable pageable = PaginationUtil.buildPageable(
            Optional.of(2), 
            Optional.of(15), 
            Optional.of("name:asc,age:desc"), 
            List.of("name", "age", "email")
        );

        assertEquals(1, pageable.getPageNumber()); // Page 2 becomes index 1
        assertEquals(15, pageable.getPageSize());
        
        Sort sort = pageable.getSort();
        assertFalse(sort.isUnsorted());
        List<Sort.Order> orders = sort.toList();
        assertEquals(2, orders.size());
        
        assertEquals("name", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());
        
        assertEquals("age", orders.get(1).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(1).getDirection());
    }
}
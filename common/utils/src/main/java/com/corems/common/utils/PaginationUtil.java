package com.corems.common.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;

public class PaginationUtil {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 1000;

    public static Pageable buildPageable(Optional<Integer> page, Optional<Integer> size, Optional<String> sort, List<String> allowedFields) {
        int pageNumber = Math.max(page.orElse(DEFAULT_PAGE), 1);
        int pageSize = Math.min(Math.max(size.orElse(DEFAULT_SIZE), 1), MAX_SIZE);
        Sort sortObj = parseSort(sort.orElse(null), List.of());
        return PageRequest.of(pageNumber - 1, pageSize, sortObj);
    }

    public static Sort parseSort(String sortParam, List<String> allowedFields) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String part : sortParam.split(",")) {
            String[] fieldDir = part.split(":");
            String field = fieldDir[0].trim();
            String direction = fieldDir.length > 1 ? fieldDir[1].trim().toLowerCase() : "desc";
            if (allowedFields.contains(field)) {
                orders.add(new Sort.Order(
                    direction.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                    field
                ));
            }
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    public static String sanitizeSearch(Optional<String> search) {
        return search.map(String::trim).filter(s -> !s.isEmpty()).orElse("");
    }
}


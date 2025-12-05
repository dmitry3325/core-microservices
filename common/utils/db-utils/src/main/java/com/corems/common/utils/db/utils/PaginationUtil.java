package com.corems.common.utils.db.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaginationUtil {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 1000;

    public static Pageable buildPageable(Optional<Integer> page, Optional<Integer> size, Optional<String> sort, List<String> allowedFields) {
        int pageOneBased = page.orElse(DEFAULT_PAGE);
        if (pageOneBased < 1) pageOneBased = DEFAULT_PAGE;
        int pageSize = Math.min(Math.max(size.orElse(DEFAULT_SIZE), 1), MAX_SIZE);
        Sort sortObj = parseSort(sort.orElse(null), allowedFields);
        return PageRequest.of(pageOneBased - 1, pageSize, sortObj);
    }

    public static Sort parseSort(String sortParam, List<String> allowedFields) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String part : sortParam.split(",")) {
            if (part.isBlank()) {
                continue;
            }
            String[] fieldDir = part.split(":");
            if (fieldDir.length == 0 || fieldDir[0].isBlank()) {
                continue;
            }
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

package com.corems.common.utils.db.spec;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class SpecificationBuilder {
    public static <T> Specification<T> build(List<FilterRequest> filters) {
        return build(filters, List.of());
    }

    public static <T> Specification<T> build(List<FilterRequest> filters, List<String> collectionFields) {
        if (filters == null || filters.isEmpty()) return null;
        Specification<T> spec = Specification.where(new GenericSpecification<>(filters.get(0), collectionFields));
        for (int i = 1; i < filters.size(); i++) {
            spec = spec.and(new GenericSpecification<>(filters.get(i), collectionFields));
        }
        return spec;
    }
}


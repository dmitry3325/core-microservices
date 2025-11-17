package com.corems.common.utils.db.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Expression;

public final class LikePredicateBuilder {
    private LikePredicateBuilder() {}

    public static Predicate[] buildLikeVariants(CriteriaBuilder cb, Path<?> path, String raw) {
        String term = raw == null ? "" : raw.toLowerCase();
        Expression<String> expr = cb.lower(path.as(String.class));
        Predicate p1 = cb.like(expr, "%" + term + "%");
        Predicate p2 = cb.like(expr, term + "%");
        Predicate p3 = cb.like(expr, "%" + term);
        return new Predicate[] { p1, p2, p3 };
    }
}


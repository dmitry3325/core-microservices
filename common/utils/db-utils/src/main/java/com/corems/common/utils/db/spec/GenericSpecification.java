package com.corems.common.utils.db.spec;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Arrays;
import java.util.List;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.Instant;
import java.util.UUID;

public class GenericSpecification<T> implements Specification<T> {
    private final FilterRequest criteria;
    private final List<String> collectionFields;

    public GenericSpecification(FilterRequest criteria) {
        this(criteria, List.of());
    }

    public GenericSpecification(FilterRequest criteria, List<String> collectionFields) {
        this.criteria = criteria;
        this.collectionFields = collectionFields != null ? collectionFields : List.of();
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Path<?> path = resolvePath(root, criteria.field());
        String raw = criteria.value() == null ? "" : criteria.value();

        return switch (criteria.op()) {
            case LIKE -> {
                Predicate[] variants = LikePredicateBuilder.buildLikeVariants(cb, path, raw);
                yield cb.or(variants);
            }
            case IN -> {
                List<Object> values = Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .map(v -> castValue(path, v))
                        .toList();
                yield path.in(values);
            }
            case CONTAINS -> {
                if (!path.getJavaType().equals(String.class)) {
                    throw ServiceException.of(DefaultExceptionReasonCodes.PARAMETER_INVALID,
                        "CONTAINS operation only supported for String fields");
                }
                String searchValue = raw.trim();
                Expression<String> paddedField = cb.concat(",", cb.concat(path.as(String.class), ","));
                Expression<String> paddedSearch = cb.concat(",", cb.concat(cb.literal(searchValue), ","));

                Predicate exactMatch = cb.equal(path, searchValue);
                Predicate containsInList = cb.like(paddedField, cb.concat("%", cb.concat(paddedSearch, "%")));

                yield cb.or(exactMatch, containsInList);
            }
            case NOT_EQUALS -> {
                Object casted = castValue(path, raw);
                yield cb.notEqual(path, casted);
            }
            case GT -> {
                Object casted = castValue(path, raw);
                yield buildGreaterThan(cb, path, casted, path.getJavaType());
            }
            case GTE -> {
                Object casted = castValue(path, raw);
                yield buildGreaterThanOrEqual(cb, path, casted, path.getJavaType());
            }
            case LT -> {
                Object casted = castValue(path, raw);
                yield buildLessThan(cb, path, casted, path.getJavaType());
            }
            case LTE -> {
                Object casted = castValue(path, raw);
                yield buildLessThanOrEqual(cb, path, casted, path.getJavaType());
            }
            default -> {
                Object casted = castValue(path, raw);
                yield cb.equal(path, casted);
            }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate buildGreaterThan(CriteriaBuilder cb, Path<?> path, Object casted, Class<?> targetType) {
        if (casted == null) return cb.conjunction();
        if (casted instanceof Number number) {
            Expression<? extends Number> numExpr = (Expression<? extends Number>) path;
            return cb.gt(numExpr, number);
        }
        if (casted instanceof Comparable comparable) {
            Expression<? extends Comparable> cmpExpr = (Expression<? extends Comparable>) path;
            return cb.greaterThan(cmpExpr, comparable);
        }
        // fallback to string compare
        return cb.greaterThan(path.as(String.class), casted.toString());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate buildGreaterThanOrEqual(CriteriaBuilder cb, Path<?> path, Object casted, Class<?> targetType) {
        if (casted == null) return cb.conjunction();
        if (casted instanceof Number number) {
            Expression<? extends Number> numExpr = (Expression<? extends Number>) path;
            return cb.ge(numExpr, number);
        }
        if (casted instanceof Comparable comparable) {
            Expression<? extends Comparable> cmpExpr = (Expression<? extends Comparable>) path;
            return cb.greaterThanOrEqualTo(cmpExpr, comparable);
        }
        return cb.greaterThanOrEqualTo(path.as(String.class), casted.toString());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate buildLessThan(CriteriaBuilder cb, Path<?> path, Object casted, Class<?> targetType) {
        if (casted == null) return cb.conjunction();
        if (casted instanceof Number number) {
            Expression<? extends Number> numExpr = (Expression<? extends Number>) path;
            return cb.lt(numExpr, number);
        }
        if (casted instanceof Comparable comparable) {
            Expression<? extends Comparable> cmpExpr = (Expression<? extends Comparable>) path;
            return cb.lessThan(cmpExpr, comparable);
        }
        return cb.lessThan(path.as(String.class), casted.toString());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate buildLessThanOrEqual(CriteriaBuilder cb, Path<?> path, Object casted, Class<?> targetType) {
        if (casted == null) return cb.conjunction();
        if (casted instanceof Number number) {
            Expression<? extends Number> numExpr = (Expression<? extends Number>) path;
            return cb.le(numExpr, number);
        }
        if (casted instanceof Comparable comparable) {
            Expression<? extends Comparable> cmpExpr = (Expression<? extends Comparable>) path;
            return cb.lessThanOrEqualTo(cmpExpr, comparable);
        }
        return cb.lessThanOrEqualTo(path.as(String.class), casted.toString());
    }

    private Path<?> resolvePath(Path<?> root, String field) {
        if (field == null || field.isBlank()) return root;

        // Check if field contains dot notation (e.g., "categories.name")
        if (field.contains(".")) {
            String[] parts = field.split("\\.");
            String basePath = parts[0];

            // Check if base path is a collection field
            if (collectionFields.contains(basePath)) {
                if (root instanceof Root<?> rootPath) {
                    // LEFT Join the collection to include entities without collection values
                    Path<?> path = rootPath.join(basePath, JoinType.LEFT);
                    // Navigate to nested fields
                    for (int i = 1; i < parts.length; i++) {
                        path = path.get(parts[i]);
                    }
                    return path;
                }
            }
            // Regular nested path - navigate normally
            Path<?> path = root;
            for (String part : parts) {
                path = path.get(part);
            }
            return path;
        }

        // Simple field - check if it's a collection
        if (collectionFields.contains(field)) {
            if (root instanceof Root<?> rootPath) {
                // Use LEFT JOIN for collection fields
                return rootPath.join(field, JoinType.LEFT);
            }
        }

        // Regular field - navigate normally
        return Arrays.stream(field.split("\\."))
                .reduce(root, Path::get, (p1, p2) -> p1);
    }

    private Object castValue(Path<?> path, String value) {
        if (value == null) return null;
        Class<?> javaType = path.getJavaType();

        // Strings
        if (javaType.equals(String.class)) return value;

        // Booleans
        if (javaType.equals(Boolean.class) || javaType.equals(boolean.class)) return Boolean.valueOf(value);

        // UUID
        if (javaType.equals(UUID.class)) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ex) {
                throw ServiceException.of(DefaultExceptionReasonCodes.PARAMETER_INVALID, "Invalid UUID value '" + value + "' for field '" + criteria.field() + "'");
            }
        }

        // Date/time
        if (javaType.equals(OffsetDateTime.class)) {
            try {
                return OffsetDateTime.parse(value);
            } catch (DateTimeParseException ex) {
                throw ServiceException.of(DefaultExceptionReasonCodes.PARAMETER_INVALID, "Invalid datetime value '" + value + "' for field '" + criteria.field() + "' (expected ISO-8601)");
            }
        }
        if (javaType.equals(Instant.class)) {
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException ex) {
                throw ServiceException.of(DefaultExceptionReasonCodes.PARAMETER_INVALID, "Invalid instant value '" + value + "' for field '" + criteria.field() + "' (expected ISO-8601)");
            }
        }

        // Numbers
        if (Number.class.isAssignableFrom(javaType) || javaType.isPrimitive()) {
            try {
                if (javaType.equals(Integer.class) || javaType.equals(int.class)) return Integer.valueOf(value);
                if (javaType.equals(Long.class) || javaType.equals(long.class)) return Long.valueOf(value);
                if (javaType.equals(Double.class) || javaType.equals(double.class)) return Double.valueOf(value);
                if (javaType.equals(Float.class) || javaType.equals(float.class)) return Float.valueOf(value);
                if (javaType.equals(java.math.BigDecimal.class)) return new java.math.BigDecimal(value);
            } catch (NumberFormatException ex) {
                throw ServiceException.of(DefaultExceptionReasonCodes.PARAMETER_INVALID, "Invalid numeric value '" + value + "' for field '" + criteria.field() + "' (expected " + javaType.getSimpleName() + ")");
            }
        }

        // Enums
        if (javaType.isEnum()) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Enum e = Enum.valueOf((Class<? extends Enum>) javaType, value);
                return e;
            } catch (Exception ex) {
                throw ServiceException.of(DefaultExceptionReasonCodes.PARAMETER_INVALID, "Invalid enum value '" + value + "' for field '" + criteria.field() + "'");
            }
        }

        return value;
    }
}

package com.corems.common.utils.db.spec;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.Instant;
import java.util.UUID;

public class GenericSpecification<T> implements Specification<T> {
    private final FilterRequest criteria;

    public GenericSpecification(FilterRequest criteria) {
        this.criteria = criteria;
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
                        .collect(Collectors.toList());
                yield path.in(values);
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

    @SuppressWarnings({"rawtypes","unchecked"})
    private Predicate buildGreaterThan(CriteriaBuilder cb, Path<?> path, Object casted, Class<?> targetType) {
        if (casted == null) return cb.conjunction();
        if (casted instanceof Number) {
            // use the path directly as a Number expression to avoid casting the column to varchar
            @SuppressWarnings("unchecked")
            Expression<? extends Number> numExpr = (Expression<? extends Number>) path;
            return cb.gt(numExpr, ((Number) casted));
        }
        if (casted instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Expression<? extends Comparable> cmpExpr = (Expression<? extends Comparable>) path;
            return cb.greaterThan(cmpExpr, (Comparable) casted);
        }
        // fallback to string compare
        return cb.greaterThan(path.as(String.class), casted.toString());
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private Predicate buildGreaterThanOrEqual(CriteriaBuilder cb, Path<?> path, Object casted, Class<?> targetType) {
        if (casted == null) return cb.conjunction();
        if (casted instanceof Number) {
            @SuppressWarnings("unchecked")
            Expression<? extends Number> numExpr = (Expression<? extends Number>) path;
            return cb.ge(numExpr, ((Number) casted));
        }
        if (casted instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Expression<? extends Comparable> cmpExpr = (Expression<? extends Comparable>) path;
            return cb.greaterThanOrEqualTo(cmpExpr, (Comparable) casted);
        }
        return cb.greaterThanOrEqualTo(path.as(String.class), casted.toString());
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private Predicate buildLessThan(CriteriaBuilder cb, Path<?> path, Object casted, Class<?> targetType) {
        if (casted == null) return cb.conjunction();
        if (casted instanceof Number) {
            @SuppressWarnings("unchecked")
            Expression<? extends Number> numExpr = (Expression<? extends Number>) path;
            return cb.lt(numExpr, ((Number) casted));
        }
        if (casted instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Expression<? extends Comparable> cmpExpr = (Expression<? extends Comparable>) path;
            return cb.lessThan(cmpExpr, (Comparable) casted);
        }
        return cb.lessThan(path.as(String.class), casted.toString());
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private Predicate buildLessThanOrEqual(CriteriaBuilder cb, Path<?> path, Object casted, Class<?> targetType) {
        if (casted == null) return cb.conjunction();
        if (casted instanceof Number) {
            @SuppressWarnings("unchecked")
            Expression<? extends Number> numExpr = (Expression<? extends Number>) path;
            return cb.le(numExpr, ((Number) casted));
        }
        if (casted instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Expression<? extends Comparable> cmpExpr = (Expression<? extends Comparable>) path;
            return cb.lessThanOrEqualTo(cmpExpr, (Comparable) casted);
        }
        return cb.lessThanOrEqualTo(path.as(String.class), casted.toString());
    }

    private Path<?> resolvePath(Path<?> root, String field) {
        if (field == null || field.isBlank()) return root;
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
            try { return UUID.fromString(value); } catch (IllegalArgumentException ex) {
                throw ServiceException.of(DefaultExceptionReasonCodes.PARAMETER_INVALID, "Invalid UUID value '" + value + "' for field '" + criteria.field() + "'");
            }
        }

        // Date/time
        if (javaType.equals(OffsetDateTime.class)) {
            try { return OffsetDateTime.parse(value); } catch (DateTimeParseException ex) {
                throw ServiceException.of(DefaultExceptionReasonCodes.PARAMETER_INVALID, "Invalid datetime value '" + value + "' for field '" + criteria.field() + "' (expected ISO-8601)");
            }
        }
        if (javaType.equals(Instant.class)) {
            try { return Instant.parse(value); } catch (DateTimeParseException ex) {
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

        // Fallback: return raw string
        return value;
    }
}

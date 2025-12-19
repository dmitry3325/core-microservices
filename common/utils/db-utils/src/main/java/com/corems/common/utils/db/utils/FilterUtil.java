package com.corems.common.utils.db.utils;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.common.utils.db.spec.FilterOperation;
import com.corems.common.utils.db.spec.FilterRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility for parsing and validating filter strings.
 * Backwards compatible parsing is provided by {@link #parse(List)}.
 * New method {@link #parseAndResolve(List, List, Map)} resolves aliases and validates against allowed list.
 */
public final class FilterUtil {
    private FilterUtil() {}

    public static List<FilterRequest> parse(List<String> rawFilters) {
        List<FilterRequest> out = new ArrayList<>();
        if (rawFilters == null) return out;
        for (String raw : rawFilters) {
            if (raw == null || raw.isBlank()) continue;
            String[] parts = raw.split(":", 3);
            String field;
            String opToken;
            String value;
            if (parts.length == 1) {
                // only field provided - no value -> skip
                continue;
            } else if (parts.length == 2) {
                field = parts[0].trim();
                opToken = "eq";
                value = parts[1];
            } else {
                field = parts[0].trim();
                opToken = parts[1].trim().toLowerCase(Locale.ROOT);
                value = parts[2];
            }

            FilterOperation op = mapOp(opToken);
            out.add(new FilterRequest(field, op, value));
        }
        return out;
    }

    public static List<FilterRequest> parseAndResolve(List<String> rawFilters, List<String> allowed, Map<String,String> aliases) {
        List<FilterRequest> parsed = parse(rawFilters);
        List<String> finalAllowed = allowed == null ? List.of() : List.copyOf(allowed);
        Map<String,String> finalAliases = aliases == null ? Map.of() : Map.copyOf(aliases);

        return parsed.stream()
                .map(fr -> {
                    String apiField = fr.field();
                    String resolved = finalAliases.getOrDefault(apiField, apiField);
                    validate(apiField, resolved, finalAllowed);
                    return new FilterRequest(resolved, fr.op(), fr.value());
                })
                .toList();
    }

    public static void validate(String apiField, String resolvedPath, List<String> allowed) {
        List<String> finalAllowed = allowed == null ? List.of() : List.copyOf(allowed);
        if (!finalAllowed.isEmpty() && !(finalAllowed.contains(apiField) || finalAllowed.contains(resolvedPath))) {
            throw ServiceException.of(DefaultExceptionReasonCodes.PROVIDED_VALUE_INVALID, "Invalid filter field: " + apiField);
        }
    }

    private static FilterOperation mapOp(String token) {
        return switch (token) {
            case "eq" -> FilterOperation.EQUALS;
            case "ne" -> FilterOperation.NOT_EQUALS;
            case "like" -> FilterOperation.LIKE;
            case "in" -> FilterOperation.IN;
            case "contains" -> FilterOperation.CONTAINS;
            case "gt" -> FilterOperation.GT;
            case "lt" -> FilterOperation.LT;
            case "gte" -> FilterOperation.GTE;
            case "lte" -> FilterOperation.LTE;
            default -> throw ServiceException.of(DefaultExceptionReasonCodes.PARAMETER_INVALID, "Unknown filter op: " + token);
        };
    }
}

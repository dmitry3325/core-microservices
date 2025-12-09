package com.corems.common.utils.db.utils;

import com.corems.common.utils.db.spec.FilterRequest;
import com.corems.common.utils.db.spec.SpecificationBuilder;
import com.corems.common.utils.db.spec.LikePredicateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.corems.common.utils.db.repo.SearchableRepository;

public final class PaginatedQueryExecutor {
    private PaginatedQueryExecutor() {}

    public static <T> Page<T> execute(
            JpaSpecificationExecutor<T> specRepo,
            QueryParams params
    ) {
        Objects.requireNonNull(specRepo);
        Objects.requireNonNull(params);

        // Ensure we have access to JpaRepository methods
        if (!(specRepo instanceof SearchableRepository<?, ?>)) {
            throw new IllegalArgumentException("Repository must implement SearchableRepository");
        }

        SearchableRepository<T, ?> searchableRepo = (SearchableRepository<T, ?>) specRepo;
        List<String> repoSearchFields = searchableRepo.getSearchFields();
        List<String> repoFilterAllowed = searchableRepo.getAllowedFilterFields();
        List<String> repoSortAllowed = searchableRepo.getAllowedSortFields();
        List<String> repoCollectionFields = searchableRepo.getCollectionFields();
        Map<String, String> aliases = searchableRepo.getFieldAliases();

        // make effectively-final copies for use inside lambdas
        final List<String> finalSearchFields = List.copyOf(repoSearchFields);
        final List<String> finalFilterAllowed = List.copyOf(repoFilterAllowed);
        final List<String> finalCollectionFields = List.copyOf(repoCollectionFields);
        final Map<String, String> finalAliases = Map.copyOf(aliases);

        // build pageable — PaginationUtil still expects allowed sort fields; pass repoSortAllowed
        Pageable pageable = PaginationUtil.buildPageable(params.page(), params.pageSize(), params.sort(), repoSortAllowed);

        // parse raw filter strings (controller provides Optional<List<String>>)
        // validate and resolve filters specification
        List<FilterRequest> resolvedFilters = FilterUtil.parseAndResolve(
                params.filters().orElse(List.of()),
                finalFilterAllowed, finalAliases);

        // build specification from filters (pass collection fields for JOIN support)
        Specification<T> spec = SpecificationBuilder.build(resolvedFilters, finalCollectionFields);

        // validate and resolve search specification if applicable
        String searchValue = PaginationUtil.sanitizeSearch(params.search());
        if (!searchValue.isEmpty() && !finalSearchFields.isEmpty()) {
            List<String> resolvedSearchFields = finalSearchFields.stream()
                    .map(f -> finalAliases.getOrDefault(f, f))
                    .toList();

            Specification<T> searchSpec = (root, query, cb) -> {
                Predicate[] preds = resolvedSearchFields.stream()
                        .filter(Objects::nonNull)
                        .map(field -> {
                            Path<?> path;

                            // Check if field contains dot notation (e.g., "categories.name")
                            if (field.contains(".")) {
                                String[] parts = field.split("\\.");
                                String basePath = parts[0];

                                // Check if base path is a collection field
                                if (finalCollectionFields.contains(basePath)) {
                                    // LEFT Join the collection to include entities without collection values
                                    path = root.join(basePath, JoinType.LEFT);
                                    // Navigate to nested fields
                                    for (int i = 1; i < parts.length; i++) {
                                        path = path.get(parts[i]);
                                    }
                                } else {
                                    // Regular nested path navigation
                                    path = root;
                                    for (String part : parts) {
                                        path = path.get(part);
                                    }
                                }
                            } else if (finalCollectionFields.contains(field)) {
                                // Simple collection field (no nesting) - use LEFT JOIN
                                path = root.join(field, JoinType.LEFT);
                            } else {
                                // Regular field
                                path = root;
                                for (String part : field.split("\\.")) {
                                    path = path.get(part);
                                }
                            }

                            // build per-field OR of variants (contains, startsWith, endsWith)
                            Predicate[] variants = LikePredicateBuilder.buildLikeVariants(cb, path, searchValue);
                            return cb.or(variants);
                        })
                        .toArray(Predicate[]::new);
                return cb.or(preds);
            };
            spec = (spec == null) ? searchSpec : spec.and(searchSpec);
        }
        
        if (spec == null) {
            return searchableRepo.findAll(pageable);
        }

        return searchableRepo.findAll(spec, pageable);
    }
}

package com.corems.common.utils.db.utils;

import com.corems.common.utils.db.spec.FilterRequest;
import com.corems.common.utils.db.spec.SpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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

        List<String> repoSearchFields = List.of();
        List<String> repoFilterAllowed = List.of();
        List<String> repoSortAllowed = List.of();
        Map<String, String> aliases = Map.of();

        if (specRepo instanceof SearchableRepository<?, ?> sr) {
            repoSearchFields = sr.getSearchFields();
            repoFilterAllowed = sr.getAllowedFilterFields();
            repoSortAllowed = sr.getAllowedSortFields();
            aliases = sr.getFieldAliases();
        }

        // make effectively-final copies for use inside lambdas
        final List<String> finalSearchFields = List.copyOf(repoSearchFields);
        final List<String> finalFilterAllowed = List.copyOf(repoFilterAllowed);
        final Map<String, String> finalAliases = Map.copyOf(aliases);

        // build pageable — PaginationUtil still expects allowed sort fields; pass repoSortAllowed
        Pageable pageable = PaginationUtil.buildPageable(params.page(), params.pageSize(), params.sort(), repoSortAllowed);

        // parse raw filter strings (controller provides Optional<List<String>>)
        // validate and resolve filters specification
        List<FilterRequest> resolvedFilters = FilterUtil.parseAndResolve(
                params.filters().orElse(List.of()),
                finalFilterAllowed, finalAliases);

        // build specification from filters
        Specification<T> spec = SpecificationBuilder.build(resolvedFilters);

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
                            Path<?> path = root;
                            for (String part : field.split("\\.")) {
                                path = path.get(part);
                            }
                            return cb.like(cb.lower(path.as(String.class)), "%" + searchValue.toLowerCase() + "%");
                        })
                        .toArray(Predicate[]::new);
                return cb.or(preds);
            };
            spec = (spec == null) ? searchSpec : spec.and(searchSpec);
        }

        return specRepo.findAll(spec, pageable);
    }
}

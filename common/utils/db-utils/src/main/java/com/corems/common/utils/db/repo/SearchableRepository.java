package com.corems.common.utils.db.repo;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.domain.Page;
import com.corems.common.utils.db.utils.QueryParams;
import com.corems.common.utils.db.utils.PaginatedQueryExecutor;

import java.util.List;
import java.util.Map;

/**
 * Contract for repositories that want to expose metadata for search/sort/filter.
 * Implementing repository interfaces can override defaults to provide per-repo allowed fields.
 */
@NoRepositoryBean
public interface SearchableRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    default List<String> getSearchFields() {
        return List.of();
    }

    default List<String> getAllowedFilterFields() {
        return List.of();
    }

    default List<String> getAllowedSortFields() {
        return List.of();
    }

    default Map<String, String> getFieldAliases() {
        return Map.of();
    }

    default Page<T> findAllByQueryParams(QueryParams params) {
        return PaginatedQueryExecutor.execute(this, params);
    }
}

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
 * Metadata-driven repository for search, filter, and sort operations.
 * <p>
 * See README.md for complete documentation and examples.
 *
 * @param <T>  the entity type
 * @param <ID> the entity ID type
 */
@NoRepositoryBean
public interface SearchableRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Fields that support free-text search (LIKE queries).
     * Supports String fields, collections, and nested paths (e.g., "categories.name").
     *
     * @return list of searchable field names
     */
    default List<String> getSearchFields() {
        return List.of();
    }

    /**
     * Fields that can be filtered.
     * Supports nested paths for collections (e.g., "categories.name").
     * Operators: eq, ne, gt, gte, lt, lte, like, contains, in
     * Types: String, Number, Boolean, Enum, Date/Time, UUID, Collections
     *
     * @return list of filterable field names
     */
    default List<String> getAllowedFilterFields() {
        return List.of();
    }

    /**
     * Fields that can be sorted (ASC/DESC).
     *
     * @return list of sortable field names
     */
    default List<String> getAllowedSortFields() {
        return List.of();
    }

    /**
     * API field name aliases (e.g., "created_at" → "createdAt").
     *
     * @return map of alias to actual field name
     */
    default Map<String, String> getFieldAliases() {
        return Map.of();
    }

    /**
     * JPA collection fields requiring JOINs (@ElementCollection, @OneToMany, @ManyToMany).
     * Declare the base collection field here; nested paths are automatically handled.
     * <p>
     * Example: Declare "categories" here, then use "categories.name" in search/filter.
     * <p>
     * <b>Important:</b> All collection fields used in search/filter MUST be declared here.
     *
     * @return list of collection field names
     */
    default List<String> getCollectionFields() {
        return List.of();
    }

    /**
     * Executes metadata-driven query with search, filter, sort, and pagination.
     *
     * @param params query parameters
     * @return page of matching entities
     */
    default Page<T> findAllByQueryParams(QueryParams params) {
        return PaginatedQueryExecutor.execute(this, params);
    }
}

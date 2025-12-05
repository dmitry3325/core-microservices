package com.corems.common.utils.db.repo;

import com.corems.common.utils.db.entity.TestProductEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface TestProductRepository extends SearchableRepository<TestProductEntity, Long> {

    @Override
    default List<String> getSearchFields() {
        return List.of("name", "sku", "categories.name", "tags");
    }

    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("name", "price", "categories.name", "categories.code", "categories.id", "tags");
    }

    @Override
    default List<String> getAllowedSortFields() {
        return List.of("name", "price");
    }

    @Override
    default List<String> getCollectionFields() {
        return List.of("categories", "tags");
    }

    @Override
    default Map<String, String> getFieldAliases() {
        return Map.of("category", "categories.name");
    }
}


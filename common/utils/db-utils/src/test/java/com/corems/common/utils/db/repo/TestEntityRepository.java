package com.corems.common.utils.db.repo;

import com.corems.common.utils.db.entity.TestEntity;

import java.util.List;

public interface TestEntityRepository extends SearchableRepository<TestEntity, Long> {
    @Override
    default List<String> getSearchFields() { return List.of("email", "firstName", "lastName"); }

    @Override
    default List<String> getAllowedFilterFields() { return List.of("email", "firstName", "lastName", "provider", "createdAt", "balance", "isActive"); }

    @Override
    default List<String> getAllowedSortFields() { return List.of("email", "firstName", "lastName", "createdAt", "balance"); }

}

package com.corems.userms.app.repository;

import com.corems.userms.app.entity.UserEntity;
import com.corems.common.utils.db.repo.SearchableRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends SearchableRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String username);

    Optional<UserEntity> findByUuid(UUID id);

    @Override
    default List<String> getSearchFields() {
        return List.of("email", "firstName", "lastName");
    }

    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("provider", "userId");
    }

    @Override
    default List<String> getAllowedSortFields() {
        return List.of("email", "provider", "firstName", "lastName", "lastLoginAt", "createdAt");
    }

    @Override
    default Map<String, String> getFieldAliases() {
        return Map.of(
                "userId", "uuid"
        );
    }
}

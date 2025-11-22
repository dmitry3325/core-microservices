package com.corems.userms.app.repository;

import com.corems.userms.app.entity.User;
import com.corems.common.utils.db.repo.SearchableRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends SearchableRepository<User, String> {
    Optional<User> findByEmail(String username);
    Optional<User> findByUuid(UUID id);

    @Override
    default List<String> getSearchFields() {
        return List.of("email", "firstName", "lastName");
    }

    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("provider", "uuid");
    }

    @Override
    default List<String> getAllowedSortFields() {
        return List.of("email", "provider", "firstName", "lastName", "lastLoginAt", "createdAt");
    }
}

package com.corems.userms.repository;

import com.corems.userms.entity.User;
import com.corems.common.utils.db.repo.SearchableRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends SearchableRepository<User, String> {
    Optional<User> findByEmail(String username);
    Optional<User> findByUuid(String id);

    @Override
    default List<String> getSearchFields() {
        return List.of("email", "firstName", "lastName");
    }

    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("provider");
    }

    @Override
    default List<String> getAllowedSortFields() {
        return List.of("email", "provider", "firstName", "lastName", "lastLoginAt", "createdAt");
    }
}

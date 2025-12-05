package com.corems.userms.app.repository;

import com.corems.userms.app.entity.LoginTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoginTokenRepository extends JpaRepository<LoginTokenEntity, String> {
    Optional<LoginTokenEntity> findByUuid(UUID uuid);
    void deleteByUuid(UUID uuid);
}
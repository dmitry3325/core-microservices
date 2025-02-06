package com.corems.userms.repository;

import com.corems.userms.entity.LoginToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginTokenRepository extends JpaRepository<LoginToken, String> {
    Optional<LoginToken> findByUuid(String uuid);
    void deleteByUuid(String uuid);
}
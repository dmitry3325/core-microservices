package com.corems.documentms.app.repository;

import com.corems.documentms.app.entity.DocumentAccessTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentAccessTokenRepository extends JpaRepository<DocumentAccessTokenEntity, Long> {

    @Query("SELECT t FROM DocumentAccessTokenEntity t WHERE t.tokenHash = :tokenHash AND t.revoked = false AND t.expiresAt > :now")
    Optional<DocumentAccessTokenEntity> findValidTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);
}


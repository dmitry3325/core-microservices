package com.corems.documentms.app.repository;

import com.corems.documentms.app.entity.DocumentAccessToken;
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
public interface DocumentAccessTokenRepository extends JpaRepository<DocumentAccessToken, Long> {

    Optional<DocumentAccessToken> findByUuid(UUID uuid);

    Optional<DocumentAccessToken> findByTokenHash(String tokenHash);

    @Query("SELECT t FROM DocumentAccessToken t WHERE t.tokenHash = :tokenHash AND t.revoked = false AND t.expiresAt > :now")
    Optional<DocumentAccessToken> findValidTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    List<DocumentAccessToken> findByDocumentUuid(UUID documentUuid);

    @Query("SELECT t FROM DocumentAccessToken t WHERE t.documentUuid = :documentUuid AND t.revoked = false AND t.expiresAt > :now")
    List<DocumentAccessToken> findValidTokensByDocumentUuid(@Param("documentUuid") UUID documentUuid, @Param("now") Instant now);

    @Query("SELECT COUNT(t) FROM DocumentAccessToken t WHERE t.documentUuid = :documentUuid AND t.revoked = false AND t.expiresAt > :now")
    long countValidTokensByDocumentUuid(@Param("documentUuid") UUID documentUuid, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM DocumentAccessToken t WHERE t.expiresAt < :before")
    void deleteExpiredTokens(@Param("before") Instant before);
}


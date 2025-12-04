package com.corems.documentms.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for storing document access tokens (for BY_LINK documents).
 * Tokens are JWT-based and stored here for validation and revocation.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DocumentAccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "document_uuid", nullable = false)
    private UUID documentUuid;

    @Column(name = "token_hash", unique = true, nullable = false)
    private String tokenHash;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked")
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "access_count")
    @Builder.Default
    private Integer accessCount = 0;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (revoked == null) {
            revoked = false;
        }
        if (accessCount == null) {
            accessCount = 0;
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}


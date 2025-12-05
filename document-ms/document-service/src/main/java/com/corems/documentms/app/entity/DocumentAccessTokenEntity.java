package com.corems.documentms.app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "document_access_token")
public class DocumentAccessTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID uuid;

    @Column(nullable = false)
    private UUID documentUuid;

    @Column(unique = true, nullable = false)
    private String tokenHash;

    private UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Builder.Default
    private Boolean revoked = false;

    private UUID revokedBy;

    private Instant revokedAt;

    @Builder.Default
    private Integer accessCount = 0;

    private Instant lastAccessedAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}


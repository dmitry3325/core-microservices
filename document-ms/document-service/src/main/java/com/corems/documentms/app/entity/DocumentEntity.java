package com.corems.documentms.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;
import java.util.LinkedHashSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "documents")
public class DocumentEntity {

    public enum Visibility {
        PUBLIC, PRIVATE, BY_LINK
    }

    public enum UploadedByType {
        USER, SYSTEM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    private String originalFilename;

    private Long size;

    private String extension;

    private String contentType;

    private String bucket;

    private String objectKey;

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    private UUID uploadedById;

    @Enumerated(EnumType.STRING)
    private UploadedByType uploadedByType;

    private Instant createdAt;

    private Instant updatedAt;

    private String checksum;

    @Column(columnDefinition = "text")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    private Boolean deleted = false;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

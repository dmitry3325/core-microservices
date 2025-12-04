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
@Table(name = "document")
public class DocumentEntity {

    public enum Visibility {
        PUBLIC("public"),
        PRIVATE("private"),
        BY_LINK("by_link");

        private final String value;

        Visibility(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum UploadedByType {
        USER("user"),
        SYSTEM("system");

        private final String value;

        UploadedByType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    private String originalFilename;

    private Long size;

    private String extension;

    private String contentType;

    private String bucket;

    private String objectKey;

    private Visibility visibility;

    private UUID uploadedById;

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

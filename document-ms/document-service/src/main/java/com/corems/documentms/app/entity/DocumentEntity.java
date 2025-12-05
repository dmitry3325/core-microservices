package com.corems.documentms.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;
import java.util.LinkedHashSet;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "document")
public class DocumentEntity {

    public enum Visibility {
        PUBLIC,
        PRIVATE,
        BY_LINK
    }

    public enum UploadedByType {
        USER,
        SYSTEM
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

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    private UUID uploadedById;

    @Enumerated(EnumType.STRING)
    private UploadedByType uploadedByType;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private String checksum;

    @Column(columnDefinition = "text")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    private Boolean deleted = false;

    private UUID deletedBy;

    private Instant deletedAt;

    @Version
    private Long version;
}

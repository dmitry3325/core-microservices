package com.corems.documentms.app.repository;

import com.corems.documentms.app.entity.DocumentEntity;
import com.corems.common.utils.db.repo.SearchableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends SearchableRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByUuid(UUID uuid);

    Optional<DocumentEntity> findByName(String name);

    // Find by UUID excluding deleted documents
    @Query("SELECT d FROM DocumentEntity d WHERE d.uuid = :uuid AND d.deleted = false")
    Optional<DocumentEntity> findByUuidExcludingDeleted(@Param("uuid") UUID uuid);

    // Find by UUID for a specific user (ownership check)
    @Query("SELECT d FROM DocumentEntity d WHERE d.uuid = :uuid AND d.uploadedById = :userId AND d.deleted = false")
    Optional<DocumentEntity> findByUuidAndUserId(@Param("uuid") UUID uuid, @Param("userId") UUID userId);

    // Find public or by-link documents (no auth required)
    @Query("SELECT d FROM DocumentEntity d WHERE d.uuid = :uuid AND d.visibility IN ('PUBLIC', 'BY_LINK') AND d.deleted = false")
    Optional<DocumentEntity> findPublicOrByLinkDocument(@Param("uuid") UUID uuid);

    // Find user's documents
    @Query("SELECT d FROM DocumentEntity d WHERE d.uploadedById = :userId AND d.deleted = false")
    Page<DocumentEntity> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    // Find user's documents including deleted (admin view)
    @Query("SELECT d FROM DocumentEntity d WHERE d.uploadedById = :userId")
    Page<DocumentEntity> findByUserIdIncludingDeleted(@Param("userId") UUID userId, Pageable pageable);

    @Override
    default List<String> getSearchFields() {
        return List.of("name", "originalFilename", "description");
    }

    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("visibility", "extension", "uploadedById", "tags", "deleted");
    }

    @Override
    default List<String> getAllowedSortFields() {
        return List.of("createdAt", "updatedAt", "size", "name");
    }

    @Override
    default Map<String, String> getFieldAliases() {
        return Map.of(
                "uploader", "uploadedById",
                "uploaded_by", "uploadedById",
                "created_at", "createdAt",
                "updated_at", "updatedAt",
                "file_name", "name"
        );
    }

}

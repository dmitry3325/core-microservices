package com.corems.documentms.app.repository;

import com.corems.documentms.app.entity.DocumentEntity;
import com.corems.common.utils.db.repo.SearchableRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends SearchableRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByUuid(UUID uuid);

    Optional<DocumentEntity> findByName(String name);

    @Override
    default List<String> getSearchFields() {
        return List.of("name", "originalFilename", "description");
    }

    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("visibility", "extension", "uploadedById", "tags");
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

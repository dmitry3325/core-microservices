package com.corems.documentms.app.service.impl;

import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.api.model.UploadBase64Request;
import com.corems.documentms.app.entity.DocumentEntity;
import com.corems.documentms.app.repository.DocumentRepository;
import com.corems.documentms.app.service.DocumentService;
import com.corems.documentms.app.service.storage.StorageService;
import com.corems.common.security.SecurityUtils;
import com.corems.common.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository repository;
    private final StorageService storage;
    private final String defaultBucket;

    public DocumentServiceImpl(DocumentRepository repository, StorageService storage,
                               @Value("${storage.default-bucket:documents}") String defaultBucket) {
        this.repository = repository;
        this.storage = storage;
        this.defaultBucket = defaultBucket;
    }

    private DocumentResponse toResponse(DocumentEntity e) {
        DocumentResponse r = new DocumentResponse();
        r.setId(e.getId() == null ? null : e.getId().intValue());
        r.setUuid(e.getUuid());
        r.setName(e.getName());
        r.setOriginalFilename(e.getOriginalFilename());
        r.setSize(e.getSize() == null ? null : e.getSize().intValue());
        r.setExtension(e.getExtension());
        r.setContentType(e.getContentType());
        r.setBucket(e.getBucket());
        r.setObjectKey(e.getObjectKey());
        r.setVisibility(com.corems.documentms.api.model.Visibility.valueOf(e.getVisibility().name()));
        r.setUploadedById(e.getUploadedById());
        r.setUploadedByType(com.corems.documentms.api.model.UploadedByType.valueOf(e.getUploadedByType().name()));
        r.setCreatedAt(e.getCreatedAt() == null ? null : OffsetDateTime.ofInstant(e.getCreatedAt(), ZoneOffset.UTC));
        r.setUpdatedAt(e.getUpdatedAt() == null ? null : OffsetDateTime.ofInstant(e.getUpdatedAt(), ZoneOffset.UTC));
        r.setChecksum(e.getChecksum());
        r.setDescription(e.getDescription());
        r.setTags(new ArrayList<>(e.getTags()));
        return r;
    }

    @Override
    @Transactional
    public DocumentResponse uploadMultipart(MultipartFile file, Optional<String> visibilityOpt, Optional<String> descriptionOpt, Optional<List<String>> tagsOpt, Optional<Boolean> confirmReplace) throws Exception {
        String name = file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
        Optional.ofNullable(repository.findByName(name)).ifPresent(opt -> {
            if (opt.isPresent() && (confirmReplace.isEmpty() || !confirmReplace.get())) {
                throw new RuntimeException("Document already exists");
            }
        });

        DocumentEntity e = new DocumentEntity();
        e.setName(name);
        e.setOriginalFilename(file.getOriginalFilename());
        e.setSize(file.getSize());
        e.setContentType(file.getContentType());
        e.setExtension(Optional.ofNullable(name).flatMap(n -> Optional.ofNullable(n.contains(".") ? n.substring(n.lastIndexOf('.') + 1) : null)).orElse(null));
        e.setBucket(defaultBucket);
        String objectKey = UUID.randomUUID().toString();
        e.setObjectKey(objectKey);
        e.setVisibility(visibilityOpt.map(DocumentEntity.Visibility::valueOf).orElse(DocumentEntity.Visibility.PRIVATE));
        e.setDescription(descriptionOpt.orElse(null));
        e.setTags(tagsOpt.map(LinkedHashSet::new).orElse(new LinkedHashSet<>()));

        // resolve identity
        try {
            UserPrincipal up = SecurityUtils.getUserPrincipalOptional().orElse(null);
            if (up != null && up.getUserId() != null) {
                e.setUploadedById(up.getUserId());
                e.setUploadedByType(DocumentEntity.UploadedByType.USER);
            } else {
                e.setUploadedByType(DocumentEntity.UploadedByType.SYSTEM);
            }
        } catch (Exception ex) {
            e.setUploadedByType(DocumentEntity.UploadedByType.SYSTEM);
        }

        // persist entity first to get id
        DocumentEntity saved = repository.save(e);

        // upload content
        storage.upload(e.getBucket(), objectKey, file.getInputStream(), file.getSize(), file.getContentType());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public DocumentResponse uploadBase64(UploadBase64Request req) throws Exception {
        byte[] data = Base64.getDecoder().decode(req.getBase64Data());
        // Create a simple in-memory MultipartFile implementation for base64 uploads (no test dependency)
        MultipartFile fallback = new org.springframework.web.multipart.MultipartFile() {
             @Override public String getName() { return req.getName(); }
             @Override public String getOriginalFilename() { return req.getName(); }
             @Override public String getContentType() { return req.getContentType(); }
             @Override public boolean isEmpty() { return data == null || data.length == 0; }
             @Override public long getSize() { return data.length; }
             @Override public byte[] getBytes() { return data; }
             @Override public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(data); }
             @Override public void transferTo(java.io.File dest) { throw new UnsupportedOperationException(); }
         };

        return uploadMultipart(fallback, Optional.ofNullable(req.getVisibility()).map(Enum::name), Optional.ofNullable(req.getDescription()), Optional.ofNullable(req.getTags()), Optional.ofNullable(req.getConfirmReplace()));
    }

    @Override
    @Transactional
    public DocumentResponse replace(UUID uuid, MultipartFile file, Optional<Boolean> confirmReplace, Optional<String> description, Optional<List<String>> tags) throws Exception {
        DocumentEntity existing = repository.findByUuid(uuid).orElseThrow(() -> new RuntimeException("Not found"));
        if (confirmReplace.isEmpty() || !confirmReplace.get()) {
            throw new RuntimeException("Replace not confirmed");
        }

        // upload new content
        storage.upload(existing.getBucket(), existing.getObjectKey(), file.getInputStream(), file.getSize(), file.getContentType());

        existing.setName(file.getOriginalFilename());
        existing.setOriginalFilename(file.getOriginalFilename());
        existing.setSize(file.getSize());
        existing.setContentType(file.getContentType());
        existing.setDescription(description.orElse(existing.getDescription()));
        existing.setTags(tags.map(LinkedHashSet::new).orElse((LinkedHashSet<String>) existing.getTags()));

        DocumentEntity saved = repository.save(existing);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID uuid, Optional<Boolean> permanent) throws Exception {
        DocumentEntity existing = repository.findByUuid(uuid).orElseThrow(() -> new RuntimeException("Not found"));
        if (permanent.isPresent() && permanent.get()) {
            storage.delete(existing.getBucket(), existing.getObjectKey());
            repository.delete(existing);
        } else {
            existing.setDeleted(true);
            repository.save(existing);
        }
    }

    @Override
    public DocumentResponse getMetadata(UUID uuid) throws Exception {
        DocumentEntity existing = repository.findByUuid(uuid).orElseThrow(() -> new RuntimeException("Not found"));
        return toResponse(existing);
    }

    @Override
    public Page<DocumentResponse> list(Optional<Integer> page, Optional<Integer> pageSize, Optional<String> sort, Optional<String> name, Optional<String> extension, Optional<com.corems.documentms.api.model.Visibility> visibility, Optional<UUID> uploadedById, Optional<List<String>> tags) {
        // Build filters list following QueryParams/FilterUtil format (field:op:value)
        List<String> filters = new ArrayList<>();
        name.ifPresent(n -> filters.add("name:like:" + n));
        extension.ifPresent(ext -> filters.add("extension:eq:" + ext));
        visibility.ifPresent(v -> filters.add("visibility:eq:" + v.name()));
        uploadedById.ifPresent(id -> filters.add("uploadedById:eq:" + id));
        tags.ifPresent(t -> t.forEach(tag -> filters.add("tags:contains:" + tag)));

        com.corems.common.utils.db.utils.QueryParams params = new com.corems.common.utils.db.utils.QueryParams(page, pageSize, Optional.empty(), sort, Optional.of(filters));
        var pageResult = repository.findAllByQueryParams(params);
        return pageResult.map(this::toResponse);
    }
}

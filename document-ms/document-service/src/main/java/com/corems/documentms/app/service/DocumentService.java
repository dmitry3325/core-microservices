package com.corems.documentms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.common.security.CoreMsRoles;
import com.corems.common.utils.db.utils.QueryParams;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.api.model.DocumentUploadMetadata;
import com.corems.documentms.api.model.GenerateLinkRequest;
import com.corems.documentms.api.model.LinkResponse;
import com.corems.documentms.api.model.PaginatedDocumentList;
import com.corems.documentms.api.model.SuccessfulResponse;
import com.corems.documentms.api.model.UploadBase64Request;
import com.corems.documentms.api.model.UploadedByType;
import com.corems.documentms.api.model.Visibility;
import com.corems.documentms.app.config.DocumentConfig;
import com.corems.documentms.app.config.StorageConfig;
import com.corems.documentms.app.entity.DocumentAccessToken;
import com.corems.documentms.app.entity.DocumentEntity;
import com.corems.documentms.app.model.DocumentStreamResult;
import com.corems.documentms.app.repository.DocumentAccessTokenRepository;
import com.corems.documentms.app.repository.DocumentRepository;
import com.corems.documentms.app.util.InMemoryMultipartFile;
import com.corems.common.security.SecurityUtils;
import com.corems.common.security.service.TokenProvider;
import com.corems.common.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final DocumentAccessTokenRepository tokenRepository;
    private final S3StorageService storage;
    private final StorageConfig storageConfig;
    private final DocumentConfig documentConfig;
    private final TokenProvider tokenProvider;

    public DocumentService(DocumentRepository repository,
                           DocumentAccessTokenRepository tokenRepository,
                           S3StorageService storage,
                           StorageConfig storageConfig,
                           DocumentConfig documentConfig,
                           TokenProvider tokenProvider) {
        this.repository = repository;
        this.tokenRepository = tokenRepository;
        this.storage = storage;
        this.storageConfig = storageConfig;
        this.documentConfig = documentConfig;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public DocumentResponse uploadMultipart(MultipartFile file, DocumentUploadMetadata metadata) {
        if (file == null || file.isEmpty()) {
            throw ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST, "File cannot be empty");
        }

        validateFileSize(file.getSize());

        String name = file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
        String extension = Optional.ofNullable(name)
                .filter(n -> n.contains("."))
                .map(n -> n.substring(n.lastIndexOf('.') + 1))
                .orElse(null);

        validateExtension(extension);

        Optional<DocumentEntity> existingDoc = repository.findByName(name);
        boolean shouldReplace = metadata != null && metadata.getConfirmReplace() != null && metadata.getConfirmReplace();

        if (existingDoc.isPresent() && !shouldReplace) {
            throw ServiceException.of(DefaultExceptionReasonCodes.CONFLICT,
                    String.format("Document with name '%s' already exists", name));
        }

        UserPrincipal principal = SecurityUtils.getUserPrincipal();

        String checksum;
        byte[] fileContent;
        try {
            fileContent = file.getBytes();
            checksum = calculateChecksum(new java.io.ByteArrayInputStream(fileContent));
        } catch (IOException e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                    "Failed to read file content: " + e.getMessage());
        }

        DocumentEntity entity;
        boolean isReplacement = false;

        if (existingDoc.isPresent()) {
            entity = existingDoc.get();
            isReplacement = true;

            checkDocumentAccess(entity);

            entity.setOriginalFilename(file.getOriginalFilename());
            entity.setContentType(file.getContentType());
            entity.setExtension(extension);
            entity.setSize(file.getSize());
            entity.setDeleted(false);
            entity.setDeletedBy(null);
            entity.setDeletedAt(null);
            entity.setChecksum(checksum);
            entity.setUpdatedAt(Instant.now());

            if (metadata.getVisibility() != null) {
                entity.setVisibility(DocumentEntity.Visibility.valueOf(metadata.getVisibility().name()));
            }
            if (metadata.getDescription() != null) {
                entity.setDescription(metadata.getDescription());
            }
            if (metadata.getTags() != null) {
                entity.setTags(normalizeTags(metadata.getTags()));
            }

            if (checksum.equals(entity.getChecksum())) {
                repository.save(entity);
                return toResponse(entity);
            }

        } else {
            entity = new DocumentEntity();
            entity.setName(name);
            entity.setOriginalFilename(file.getOriginalFilename());
            entity.setSize(file.getSize());
            entity.setContentType(file.getContentType());
            entity.setExtension(extension);
            entity.setBucket(storageConfig.getDefaultBucket());
            entity.setChecksum(checksum);

            UUID documentUuid = UUID.randomUUID();
            UUID ownerId = (metadata != null && metadata.getOwnerUserId() != null)
                    ? metadata.getOwnerUserId()
                    : principal.getUserId();

            if (metadata != null && metadata.getOwnerUserId() != null) {
                if (!SecurityUtils.hasRole(CoreMsRoles.DOCUMENT_MS_ADMIN)) {
                    throw ServiceException.of(DefaultExceptionReasonCodes.FORBIDDEN,
                            "Only administrators can create documents for other users");
                }
            }

            String objectKey = ownerId != null
                    ? ownerId + "/" + documentUuid
                    : "system/" + documentUuid;

            entity.setUserId(ownerId);
            entity.setObjectKey(objectKey);
            entity.setUuid(documentUuid);
            entity.setVisibility(metadata != null && metadata.getVisibility() != null
                    ? DocumentEntity.Visibility.valueOf(metadata.getVisibility().name())
                    : DocumentEntity.Visibility.PRIVATE);
            entity.setDescription(metadata != null ? metadata.getDescription() : null);
            entity.setTags(normalizeTags(metadata != null ? metadata.getTags() : null));

            if (ownerId != null) {
                entity.setUploadedById(ownerId);
                entity.setUploadedByType(DocumentEntity.UploadedByType.USER);
            } else {
                entity.setUploadedByType(DocumentEntity.UploadedByType.SYSTEM);
            }
        }

        DocumentEntity saved = repository.save(entity);

        try {
            storage.upload(entity.getBucket(), entity.getObjectKey(),
                    new ByteArrayInputStream(fileContent),
                    file.getSize(), file.getContentType());
        } catch (ServiceException ex) {
            if (!isReplacement) {
                repository.delete(saved);
            }
            throw ex;
        }

        return toResponse(saved);
    }

    @Transactional
    public DocumentResponse uploadBase64(UploadBase64Request req) {
        if (req == null || req.getBase64Data() == null || req.getBase64Data().isBlank()) {
            throw ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST, "Base64 data cannot be empty");
        }

        byte[] data;
        try {
            data = Base64.getDecoder().decode(req.getBase64Data());
        } catch (IllegalArgumentException e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                    "Invalid base64 encoding: " + e.getMessage());
        }

        MultipartFile file = new InMemoryMultipartFile(
                req.getName(),
                req.getName(),
                req.getContentType(),
                data
        );

        DocumentUploadMetadata metadata = new DocumentUploadMetadata();
        metadata.setOwnerUserId(req.getOwnerUserId());
        metadata.setVisibility(req.getVisibility());
        metadata.setDescription(req.getDescription());
        metadata.setTags(req.getTags());
        metadata.setConfirmReplace(req.getConfirmReplace());

        return uploadMultipart(file, metadata);
    }

    @Transactional
    public SuccessfulResponse delete(UUID uuid, Boolean permanent) {
        DocumentEntity existing = repository.findByUuid(uuid)
                .orElseThrow(() -> ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                        "Document not found with UUID: " + uuid));

        checkDocumentAccess(existing);

        UserPrincipal principal = SecurityUtils.getUserPrincipal();

        if (Boolean.TRUE.equals(permanent)) {
            if (SecurityUtils.hasRole(CoreMsRoles.SUPER_ADMIN)) {
                throw ServiceException.of(DefaultExceptionReasonCodes.FORBIDDEN,
                        "Only system administrators can perform permanent deletions");
            }

            try {
                storage.delete(existing.getBucket(), existing.getObjectKey());
            } catch (Exception e) {
                throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                        "Failed to delete document from storage: " + e.getMessage());
            }
            repository.delete(existing);
        } else {
            // Soft delete with audit trail
            existing.setDeleted(true);
            existing.setDeletedBy(principal.getUserId());
            existing.setDeletedAt(Instant.now());
            repository.save(existing);
        }

        return new SuccessfulResponse().result(true);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getByUuid(UUID uuid) {
        DocumentEntity entity = repository.findByUuid(uuid)
                .orElseThrow(() -> ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                        "Document not found with UUID: " + uuid));

        checkDocumentAccess(entity);

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public DocumentStreamResult prepareStreamResponse(UUID uuid) {
        DocumentEntity entity = repository.findByUuid(uuid)
                .orElseThrow(() -> ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                        "Document not found with UUID: " + uuid));

        checkDocumentAccess(entity);

        InputStream stream;
        try {
            stream = storage.download(entity.getBucket(), entity.getObjectKey());
        } catch (Exception e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                    "Failed to download document from storage: " + e.getMessage());
        }

        return DocumentStreamResult.builder()
                .stream(stream)
                .contentType(entity.getContentType())
                .size(entity.getSize())
                .filename(entity.getName())
                .build();
    }

    @Transactional
    public LinkResponse generateAccessLink(UUID uuid, GenerateLinkRequest request) {
        DocumentEntity entity = repository.findByUuid(uuid)
                .orElseThrow(() -> ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                        "Document not found with UUID: " + uuid));

        if (entity.getVisibility() != DocumentEntity.Visibility.BY_LINK) {
            throw ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                    "Access links can only be generated for BY_LINK visibility documents");
        }

        checkDocumentAccess(entity);

        UserPrincipal principal = SecurityUtils.getUserPrincipal();
        int expirySeconds = request != null && request.getExpiresInSeconds() != null
                ? request.getExpiresInSeconds()
                : 3600;

        Instant expiresAt = Instant.now().plusSeconds(expirySeconds);

        // Generate JWT token with document UUID as subject
        String jwtToken = tokenProvider.createAccessToken(
                entity.getUuid().toString(),
                Map.of(
                        "type", "document_access",
                        "documentUuid", entity.getUuid().toString(),
                        "visibility", entity.getVisibility().name(),
                        "expiresIn", expirySeconds
                )
        );

        // Hash the token for storage (store hash, not the actual token)
        String tokenHash = hashToken(jwtToken);

        // Store token record in database for validation and revocation
        DocumentAccessToken tokenRecord = DocumentAccessToken.builder()
                .documentUuid(entity.getUuid())
                .tokenHash(tokenHash)
                .createdBy(principal.getUserId())
                .expiresAt(expiresAt)
                .build();

        tokenRepository.save(tokenRecord);

        LinkResponse response = new LinkResponse();
        response.setUrl(buildDocumentAccessUrl(jwtToken));
        response.setToken(jwtToken);
        response.setExpiresAt(OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));

        return response;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Token hashing failed");
        }
    }

    private String buildDocumentAccessUrl(String token) {
        // In production, this should use the actual base URL from configuration
        return "/api/public/documents/link/" + token;
    }

    public PaginatedDocumentList getDocumentList(
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> search,
            Optional<String> sort,
            Optional<List<String>> filters,
            Optional<Boolean> includeDeleted) {

        UserPrincipal principal = SecurityUtils.getUserPrincipal();
        boolean isAdmin = SecurityUtils.hasRole(CoreMsRoles.DOCUMENT_MS_ADMIN);

        if (sort.isEmpty()) {
            sort = Optional.of("createdAt:desc");
        }

        // Add filter for non-deleted documents unless includeDeleted is true and user is admin
        List<String> filterList = new ArrayList<>(filters.orElse(new ArrayList<>()));
        if (!includeDeleted.orElse(false) || !isAdmin) {
            filterList.add("deleted:false");
        }

        // Regular users (DOCUMENTMS_USER) can only see their own documents
        if (!isAdmin && principal.getUserId() != null) {
            filterList.add("uploadedById:" + principal.getUserId().toString());
        }

        QueryParams params = new QueryParams(page, pageSize, search, sort, Optional.of(filterList));
        var pageResult = repository.findAllByQueryParams(params);

        PaginatedDocumentList pl = new PaginatedDocumentList(page.orElse(1), pageSize.orElse(10));
        List<DocumentResponse> items = pageResult.getContent().stream()
                .map(this::toResponse)
                .toList();
        pl.setItems(items);
        pl.setTotalElements(pageResult.getTotalElements());
        pl.setTotalPages(pageResult.getTotalPages());

        return pl;
    }

    private String calculateChecksum(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Checksum calculation failed");
        }
    }

    private Set<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateFileSize(long size) {
        if (size > documentConfig.getMaxUploadSize()) {
            throw ServiceException.of(
                    DefaultExceptionReasonCodes.INVALID_REQUEST,
                    String.format("File size %d bytes exceeds maximum allowed size of %d bytes",
                            size, documentConfig.getMaxUploadSize())
            );
        }
    }

    private void validateExtension(String extension) {
        if (extension != null && !extension.isBlank()) {
            String ext = extension.toLowerCase().trim();
            if (!documentConfig.getAllowedExtensionsSet().contains(ext)) {
                throw ServiceException.of(
                        DefaultExceptionReasonCodes.INVALID_REQUEST,
                        String.format("File extension '%s' is not allowed. Allowed extensions: %s",
                                ext, String.join(", ", documentConfig.getAllowedExtensionsSet()))
                );
            }
        }
    }

    private void checkDocumentAccess(DocumentEntity document) {
        UserPrincipal principal = SecurityUtils.getUserPrincipalOptional().orElse(null);

        // PUBLIC documents are accessible to everyone
        if (document.getVisibility() == DocumentEntity.Visibility.PUBLIC) {
            return;
        }

        // Admin can access everything
        if (principal != null && principal.getAuthorities() != null &&
            principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(CoreMsRoles.DOCUMENT_MS_ADMIN.name()))) {
            return;
        }

        // PRIVATE documents - only owner can access
        if (document.getVisibility() == DocumentEntity.Visibility.PRIVATE) {
            if (principal == null || principal.getUserId() == null) {
                throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED,
                        "Authentication required to access private documents");
            }
            if (!document.getUploadedById().equals(principal.getUserId())) {
                throw ServiceException.of(DefaultExceptionReasonCodes.FORBIDDEN,
                        "You don't have permission to access this document");
            }
        }

        // BY_LINK documents - need valid token (handled elsewhere) or ownership
        if (document.getVisibility() == DocumentEntity.Visibility.BY_LINK) {
            if (principal != null && principal.getUserId() != null &&
                document.getUploadedById().equals(principal.getUserId())) {
                return; // Owner can access
            }
            // Otherwise, access must be through token (public endpoint)
        }
    }

    private DocumentResponse toResponse(DocumentEntity e) {
        DocumentResponse r = new DocumentResponse();
        r.setUuid(e.getUuid());
        r.setName(e.getName());
        r.setOriginalFilename(e.getOriginalFilename());
        r.setSize(e.getSize() == null ? null : e.getSize().intValue());
        r.setExtension(e.getExtension());
        r.setContentType(e.getContentType());
        r.setBucket(e.getBucket());
        r.setObjectKey(e.getObjectKey());
        r.setVisibility(Visibility.valueOf(e.getVisibility().name()));
        r.setUploadedById(e.getUploadedById());
        r.setUploadedByType(UploadedByType.valueOf(e.getUploadedByType().name()));
        r.setCreatedAt(e.getCreatedAt() == null ? null : OffsetDateTime.ofInstant(e.getCreatedAt(), ZoneOffset.UTC));
        r.setUpdatedAt(e.getUpdatedAt() == null ? null : OffsetDateTime.ofInstant(e.getUpdatedAt(), ZoneOffset.UTC));
        r.setChecksum(e.getChecksum());
        r.setDescription(e.getDescription());
        r.setTags(new ArrayList<>(e.getTags()));

        if (e.getDeleted() != null && e.getDeleted()) {
            r.setDeleted(true);
            r.setDeletedBy(e.getDeletedBy());
            r.setDeletedAt(e.getDeletedAt() == null ? null : OffsetDateTime.ofInstant(e.getDeletedAt(), ZoneOffset.UTC));
        }

        return r;
    }


}

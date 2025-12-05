package com.corems.documentms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.common.security.service.TokenProvider;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.api.model.UploadedByType;
import com.corems.documentms.api.model.Visibility;
import com.corems.documentms.app.config.StorageConfig;
import com.corems.documentms.app.entity.DocumentAccessTokenEntity;
import com.corems.documentms.app.entity.DocumentEntity;
import com.corems.documentms.app.model.DocumentStreamResult;
import com.corems.documentms.app.repository.DocumentAccessTokenRepository;
import com.corems.documentms.app.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Service for public document access (no authentication required).
 * Handles PUBLIC and BY_LINK visibility documents.
 */
@Service
public class PublicDocumentService {

    private final DocumentRepository repository;
    private final DocumentAccessTokenRepository tokenRepository;
    private final S3StorageService storage;
    private final StorageConfig storageConfig;
    private final TokenProvider tokenProvider;

    public PublicDocumentService(DocumentRepository repository,
                                 DocumentAccessTokenRepository tokenRepository,
                                 S3StorageService storage,
                                 StorageConfig storageConfig,
                                 TokenProvider tokenProvider) {
        this.repository = repository;
        this.tokenRepository = tokenRepository;
        this.storage = storage;
        this.storageConfig = storageConfig;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public DocumentResponse getPublicDocumentMetadata(UUID uuid) {
        DocumentEntity entity = repository.findPublicOrByLinkDocument(uuid)
                .orElseThrow(() -> ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                        "Document not found or not accessible"));

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public DocumentStreamResult preparePublicDocumentStream(UUID uuid) {
        DocumentEntity entity = repository.findPublicOrByLinkDocument(uuid)
                .orElseThrow(() -> ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                        "Document not found or not accessible"));

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
                .filename(entity.getOriginalFilename())
                .build();
    }

    @Transactional
    public DocumentResponse getDocumentByToken(String token) {
        // Validate JWT token
        if (!tokenProvider.isTokenValid(token)) {
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED,
                    "Invalid or expired token");
        }

        // Extract document UUID from token claims
        String documentUuidStr = tokenProvider.getClaim(token, claims ->
                claims.get("documentUuid", String.class));

        if (documentUuidStr == null) {
            throw ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                    "Token does not contain document information");
        }

        UUID documentUuid;
        try {
            documentUuid = UUID.fromString(documentUuidStr);
        } catch (IllegalArgumentException e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                    "Invalid document UUID in token");
        }

        // Hash the token to look up in database
        String tokenHash = hashToken(token);

        // Verify token exists in database and is valid
        DocumentAccessTokenEntity tokenRecord = tokenRepository.findValidTokenByHash(tokenHash, Instant.now())
                .orElseThrow(() -> ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED,
                        "Token not found, expired, or has been revoked"));

        // Track token usage
        tokenRecord.setAccessCount(tokenRecord.getAccessCount() + 1);
        tokenRecord.setLastAccessedAt(Instant.now());
        tokenRepository.save(tokenRecord);

        // Get the document
        DocumentEntity entity = repository.findPublicOrByLinkDocument(documentUuid)
                .orElseThrow(() -> ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                        "Document not found or not accessible"));

        return toResponse(entity);
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
        if (e.getTags() == null || e.getTags().isEmpty()) {
            r.setTags(null);
        } else {
            r.setTags(String.join(",", e.getTags()));
        }
        return r;
    }
}

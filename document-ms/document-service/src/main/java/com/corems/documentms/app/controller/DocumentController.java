package com.corems.documentms.app.controller;

import com.corems.documentms.api.DocumentApi;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.api.model.GenerateLinkRequest;
import com.corems.documentms.api.model.LinkResponse;
import com.corems.documentms.api.model.SuccessfulResponse;
import com.corems.documentms.app.service.DocumentService;
import com.corems.documentms.app.service.storage.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class DocumentController implements DocumentApi {

    private final DocumentService service;
    private final StorageService storage;

    public DocumentController(DocumentService service, StorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @Override
    public ResponseEntity<SuccessfulResponse> deleteDocument(UUID uuid, Optional<Boolean> permanent) {
        try {
            service.delete(uuid, permanent);
            SuccessfulResponse r = new SuccessfulResponse();
            r.setResult(true);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<LinkResponse> generateDocumentAccessLink(UUID uuid, GenerateLinkRequest generateLinkRequest) {
        try {
            var doc = service.getMetadata(uuid);
            if (doc == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            if (doc.getVisibility() != com.corems.documentms.api.model.Visibility.BY_LINK) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            int expires = (generateLinkRequest != null && generateLinkRequest.getExpiresInSeconds() != null) ? generateLinkRequest.getExpiresInSeconds() : 3600;
            String url = storage.generatePresignedUrl(doc.getBucket(), doc.getObjectKey(), expires);
            LinkResponse lr = new LinkResponse();
            lr.setToken(url);
            lr.setUrl(url);
            lr.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(expires));
            return ResponseEntity.ok(lr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<DocumentResponse> getDocumentMetadata(UUID uuid) {
        try {
            return ResponseEntity.ok(service.getMetadata(uuid));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<DocumentResponse> replaceDocument(UUID uuid, Boolean confirmReplace, MultipartFile file, String description, List<String> tags) {
        try {
            var res = service.replace(uuid, file, Optional.ofNullable(confirmReplace), Optional.ofNullable(description), Optional.ofNullable(tags));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<Void> streamDocumentByUuid(UUID uuid) {
        // Stream not implemented yet - will be implemented in storage service
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}

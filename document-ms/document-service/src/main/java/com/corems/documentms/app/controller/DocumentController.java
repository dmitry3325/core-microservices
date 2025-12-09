package com.corems.documentms.app.controller;

import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.RequireRoles;
import com.corems.documentms.api.DocumentApi;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.api.model.DocumentUpdateRequest;
import com.corems.documentms.api.model.GenerateLinkRequest;
import com.corems.documentms.api.model.LinkResponse;
import com.corems.documentms.api.model.SuccessfulResponse;
import com.corems.documentms.app.config.DocumentConfig;
import com.corems.documentms.app.model.DocumentStreamResult;
import com.corems.documentms.app.service.DocumentService;
import com.corems.documentms.app.util.StreamResponseHelper;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
public class DocumentController implements DocumentApi {

    private final DocumentService service;
    private final DocumentConfig documentConfig;

    public DocumentController(DocumentService service, DocumentConfig documentConfig) {
        this.service = service;
        this.documentConfig = documentConfig;
    }

    @Override
    public ResponseEntity<LinkResponse> generateDocumentAccessLink(UUID uuid, Optional<GenerateLinkRequest> generateLinkRequest) {
        return ResponseEntity.ok(service.generateAccessLink(uuid, generateLinkRequest.orElse(null)));
    }

    @Override
    public ResponseEntity<DocumentResponse> getDocumentMetadata(UUID uuid) {
        return ResponseEntity.ok(service.getByUuid(uuid));
    }

    @Override
    @RequireRoles(CoreMsRoles.DOCUMENT_MS_ADMIN)
    public ResponseEntity<DocumentResponse> updateDocumentMetadata(UUID uuid, Optional<DocumentUpdateRequest> documentUpdateRequest) {
        return ResponseEntity.ok(service.updateMetadata(uuid, documentUpdateRequest.orElse(null)));
    }

    @Override
    @RequireRoles(CoreMsRoles.DOCUMENT_MS_ADMIN)
    public ResponseEntity<SuccessfulResponse> deleteDocument(UUID uuid, Optional<Boolean> permanent) {
        return ResponseEntity.ok(service.delete(uuid, permanent.orElse(false)));
    }

    @Override
    public ResponseEntity<Resource> streamDocumentByUuid(UUID uuid) {
        DocumentStreamResult streamResult = service.prepareStreamResponse(uuid);
        return StreamResponseHelper.buildStreamResponse(streamResult, documentConfig, "attachment");
    }
}

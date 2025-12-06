package com.corems.documentms.app.controller;

import com.corems.documentms.api.PublicDocumentsApi;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.app.config.DocumentConfig;
import com.corems.documentms.app.model.DocumentStreamResult;
import com.corems.documentms.app.service.PublicDocumentService;
import com.corems.documentms.app.util.StreamResponseHelper;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PublicDocumentsController implements PublicDocumentsApi {

    private final PublicDocumentService service;
    private final DocumentConfig documentConfig;

    public PublicDocumentsController(PublicDocumentService service, DocumentConfig documentConfig) {
        this.service = service;
        this.documentConfig = documentConfig;
    }

    @Override
    public ResponseEntity<DocumentResponse> getPublicDocumentMetadata(UUID uuid) {
        return ResponseEntity.ok(service.getPublicDocumentMetadata(uuid));
    }

    @Override
    public ResponseEntity<Resource> downloadPublicDocument(UUID uuid) {
        DocumentStreamResult streamResult = service.preparePublicDocumentStream(uuid);
        return StreamResponseHelper.buildStreamResponse(streamResult, documentConfig, "inline");
    }

    @Override
    public ResponseEntity<Resource> accessDocumentByToken(String token) {
        DocumentStreamResult streamResult = service.prepareStreamByToken(token);
        return StreamResponseHelper.buildStreamResponse(streamResult, documentConfig, "inline");
    }
}

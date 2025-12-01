package com.corems.documentms.app.controller;

import com.corems.documentms.api.PublicDocumentsApi;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.app.config.DocumentConfig;
import com.corems.documentms.app.model.DocumentStreamResult;
import com.corems.documentms.app.service.PublicDocumentService;
import com.corems.documentms.app.util.BufferedStreamingResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                streamResult.getContentType() != null ? streamResult.getContentType() : "application/octet-stream"));
        headers.setContentLength(streamResult.getSize() != null ? streamResult.getSize() : -1);
        headers.setContentDispositionFormData("attachment", streamResult.getFilename());
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        BufferedStreamingResource resource = new BufferedStreamingResource(
                streamResult.getStream(),
                documentConfig.getStream().getBufferSize());

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @Override
    public ResponseEntity<DocumentResponse> accessDocumentByToken(String token) {
        return ResponseEntity.ok(service.getDocumentByToken(token));
    }
}


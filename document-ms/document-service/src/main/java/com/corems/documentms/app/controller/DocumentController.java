package com.corems.documentms.app.controller;

import com.corems.documentms.api.DocumentApi;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.api.model.GenerateLinkRequest;
import com.corems.documentms.api.model.LinkResponse;
import com.corems.documentms.api.model.SuccessfulResponse;
import com.corems.documentms.app.config.DocumentConfig;
import com.corems.documentms.app.model.DocumentStreamResult;
import com.corems.documentms.app.service.DocumentService;
import com.corems.documentms.app.util.BufferedStreamingResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    public ResponseEntity<SuccessfulResponse> deleteDocument(UUID uuid, Optional<Boolean> permanent) {
        return ResponseEntity.ok(service.delete(uuid, permanent.orElse(false)));
    }

    @Override
    public ResponseEntity<LinkResponse> generateDocumentAccessLink(UUID uuid, GenerateLinkRequest generateLinkRequest) {
        return ResponseEntity.ok(service.generateAccessLink(uuid, generateLinkRequest));
    }

    @Override
    public ResponseEntity<DocumentResponse> getDocumentMetadata(UUID uuid) {
        return ResponseEntity.ok(service.getByUuid(uuid));
    }

    @Override
    public ResponseEntity<Resource> streamDocumentByUuid(UUID uuid) {
        DocumentStreamResult streamResult = service.prepareStreamResponse(uuid);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                streamResult.getContentType() != null ? streamResult.getContentType() : "application/octet-stream"));
        headers.setContentLength(streamResult.getSize() != null ? streamResult.getSize() : -1);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(streamResult.getFilename())
                .build());
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
}



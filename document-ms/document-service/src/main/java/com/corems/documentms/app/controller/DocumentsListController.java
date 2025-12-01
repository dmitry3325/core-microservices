package com.corems.documentms.app.controller;

import com.corems.documentms.api.DocumentsListApi;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.api.model.DocumentUploadMetadata;
import com.corems.documentms.api.model.UploadBase64Request;
import com.corems.documentms.api.model.PaginatedDocumentList;
import com.corems.documentms.api.model.Visibility;
import com.corems.documentms.app.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class DocumentsListController implements DocumentsListApi {

    private final DocumentService service;

    public DocumentsListController(DocumentService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<DocumentResponse> uploadDocumentMultipart(MultipartFile file,
                                                                    UUID ownerUserId,
                                                                    Visibility visibility,
                                                                    String description,
                                                                    List<String> tags,
                                                                    Boolean confirmReplace) {
        DocumentUploadMetadata metadata = new DocumentUploadMetadata();
        metadata.setOwnerUserId(ownerUserId);
        metadata.setVisibility(visibility);
        metadata.setDescription(description);
        metadata.setTags(tags);
        metadata.setConfirmReplace(confirmReplace);

        DocumentResponse response = service.uploadMultipart(file, metadata);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<DocumentResponse> uploadDocumentBase64(UploadBase64Request uploadBase64Request) {
        DocumentResponse response = service.uploadBase64(uploadBase64Request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<PaginatedDocumentList> listDocuments(Optional<Integer> page,
                                                                Optional<Integer> pageSize,
                                                                Optional<String> sort,
                                                                Optional<String> search,
                                                                Optional<List<String>> filters,
                                                                Optional<Boolean> includeDeleted) {
        return ResponseEntity.ok(service.getDocumentList(page, pageSize, search, sort, filters, includeDeleted));
    }
}

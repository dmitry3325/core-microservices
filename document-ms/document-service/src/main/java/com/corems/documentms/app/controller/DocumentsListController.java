package com.corems.documentms.app.controller;

import com.corems.documentms.api.DocumentsListApi;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.api.model.UploadBase64Request;
import com.corems.documentms.api.model.PaginatedDocumentList;
import com.corems.documentms.api.model.Visibility;
import com.corems.documentms.app.service.DocumentService;
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
                                                                    Visibility visibility,
                                                                    String description,
                                                                    List<String> tags,
                                                                    Boolean confirmReplace) {
        try {
            DocumentResponse res = service.uploadMultipart(
                    file,
                    Optional.ofNullable(visibility).map(Enum::name),
                    Optional.ofNullable(description),
                    Optional.ofNullable(tags),
                    Optional.ofNullable(confirmReplace)
            );
            return ResponseEntity.status(201).body(res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<DocumentResponse> uploadDocumentBase64(UploadBase64Request uploadBase64Request) {
        try {
            var res = service.uploadBase64(uploadBase64Request);
            return ResponseEntity.status(201).body(res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<PaginatedDocumentList> listDocuments(Optional<Integer> page, Optional<Integer> pageSize, Optional<String> sort, Optional<List<String>> tags, Optional<String> name, Optional<String> extension, Optional<com.corems.documentms.api.model.Visibility> visibility, Optional<UUID> uploadedById) {
        var pageRes = service.list(page, pageSize, sort, name, extension, visibility, uploadedById, tags);
        PaginatedDocumentList pl = new PaginatedDocumentList(page.orElse(1), pageSize.orElse(10));
        pl.setItems(pageRes.getContent());
        pl.setTotalElements(pageRes.getTotalElements());
        pl.setTotalPages(pageRes.getTotalPages());
        return ResponseEntity.ok(pl);
    }
}

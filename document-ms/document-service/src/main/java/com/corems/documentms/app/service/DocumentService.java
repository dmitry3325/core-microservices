package com.corems.documentms.app.service;

import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.api.model.UploadBase64Request;
import com.corems.documentms.app.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

public interface DocumentService {
    DocumentResponse uploadMultipart(MultipartFile file, Optional<String> visibility, Optional<String> description, Optional<java.util.List<String>> tags, Optional<Boolean> confirmReplace) throws Exception;
    DocumentResponse uploadBase64(UploadBase64Request req) throws Exception;
    DocumentResponse replace(UUID uuid, MultipartFile file, Optional<Boolean> confirmReplace, Optional<String> description, Optional<java.util.List<String>> tags) throws Exception;
    void delete(UUID uuid, Optional<Boolean> permanent) throws Exception;
    DocumentResponse getMetadata(UUID uuid) throws Exception;
    Page<DocumentResponse> list(Optional<Integer> page, Optional<Integer> pageSize, Optional<String> sort, Optional<String> name, Optional<String> extension, Optional<com.corems.documentms.api.model.Visibility> visibility, Optional<UUID> uploadedById, Optional<java.util.List<String>> tags);
}


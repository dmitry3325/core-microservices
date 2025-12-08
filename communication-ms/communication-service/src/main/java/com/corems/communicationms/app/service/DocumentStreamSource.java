package com.corems.communicationms.app.service;

import com.corems.documentms.client.DocumentApi;
import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.NonNull;

/**
 * InputStreamSource that creates a fresh InputStream on each call.
 * It either buffers to memory (if small) or uses a temp file returned by the document client.
 */
public class DocumentStreamSource implements InputStreamSource {
    private final UUID documentUuid;
    private final DocumentApi documentApi;
    private final int maxInMemory;

    public DocumentStreamSource(UUID documentUuid, DocumentApi documentApi, int maxInMemory) {
        this.documentUuid = documentUuid;
        this.documentApi = documentApi;
        this.maxInMemory = maxInMemory;
    }

    @Override
    @NonNull
    public InputStream getInputStream() {
        try {
            var meta = documentApi.getDocumentMetadata(documentUuid).block();
            if (meta != null && meta.getSize() != null && meta.getSize() <= maxInMemory) {
                byte[] bytes = documentApi.streamDocumentByUuidWithResponseSpec(documentUuid).bodyToMono(byte[].class).block();
                if (bytes == null) throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Empty document: " + documentUuid);
                return new ByteArrayInputStream(bytes);
            }

            // fallback - stream to a temp file once and return a fresh FileInputStream
            File tmp = documentApi.streamDocumentByUuid(documentUuid).block();
            if (tmp == null || !tmp.exists()) throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Document content unavailable: " + documentUuid);
            return new FileInputStream(tmp);
        } catch (Exception ex) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Unable to open document stream: " + documentUuid);
        }
    }
}

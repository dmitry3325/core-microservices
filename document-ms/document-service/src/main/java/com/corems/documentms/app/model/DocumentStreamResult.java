package com.corems.documentms.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

/**
 * Internal model for document streaming operations.
 * Contains the stream and metadata needed for HTTP response.
 * This is NOT part of the API contract, hence kept as internal model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStreamResult {
    private InputStream stream;
    private String contentType;
    private Long size;
    private String filename;
}


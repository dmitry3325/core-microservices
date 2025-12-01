package com.corems.documentms.app.util;

import org.springframework.core.io.InputStreamResource;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Custom streaming resource with explicit buffer size control.
 * Uses BufferedInputStream to ensure efficient streaming of large files
 * without loading the entire content into memory.
 */
public class BufferedStreamingResource extends InputStreamResource {

    private static final int DEFAULT_BUFFER_SIZE = 8192; // 8KB buffer

    private final int bufferSize;

    public BufferedStreamingResource(InputStream inputStream) {
        this(inputStream, DEFAULT_BUFFER_SIZE);
    }

    public BufferedStreamingResource(InputStream inputStream, int bufferSize) {
        super(new BufferedInputStream(inputStream, bufferSize));
        this.bufferSize = bufferSize;
    }

    @Override
    public String getDescription() {
        return "Buffered streaming resource with buffer size: " + bufferSize + " bytes";
    }

    @Override
    public long contentLength() {
        return -1;
    }
}


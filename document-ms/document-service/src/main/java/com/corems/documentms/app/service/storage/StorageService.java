package com.corems.documentms.app.service.storage;

import java.io.InputStream;

public interface StorageService {
    String upload(String bucket, String objectKey, InputStream data, long length, String contentType) throws Exception;
    InputStream download(String bucket, String objectKey) throws Exception;
    void delete(String bucket, String objectKey) throws Exception;
    /**
     * Generate a presigned URL for the given object with expiry in seconds.
     */
    String generatePresignedUrl(String bucket, String objectKey, int expiresInSeconds) throws Exception;
}

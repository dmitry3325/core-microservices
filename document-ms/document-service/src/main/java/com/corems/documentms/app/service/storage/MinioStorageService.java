package com.corems.documentms.app.service.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
@Service
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final String defaultBucket;

    public MinioStorageService(
            @Value("${storage.minio.endpoint}") String endpoint,
            @Value("${storage.minio.accessKey}") String accessKey,
            @Value("${storage.minio.secretKey}") String secretKey,
            @Value("${storage.default-bucket:documents}") String defaultBucket
    ) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.defaultBucket = defaultBucket;
    }

    @Override
    public String upload(String bucket, String objectKey, InputStream data, long length, String contentType) throws Exception {
        if (bucket == null || bucket.isBlank()) bucket = defaultBucket;
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(data, length, -1)
                .contentType(contentType)
                .build());
        return objectKey;
    }

    @Override
    public InputStream download(String bucket, String objectKey) throws Exception {
        if (bucket == null || bucket.isBlank()) bucket = defaultBucket;
        return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
    }

    @Override
    public void delete(String bucket, String objectKey) throws Exception {
        if (bucket == null || bucket.isBlank()) bucket = defaultBucket;
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    }

    @Override
    public String generatePresignedUrl(String bucket, String objectKey, int expiresInSeconds) throws Exception {
        if (bucket == null || bucket.isBlank()) bucket = defaultBucket;
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectKey)
                        .expiry(expiresInSeconds)
                        .build()
        );
    }
}

package com.corems.documentms.app.service.storage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;

@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
@Service
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final String defaultBucket;
    private final String endpoint;

    public S3StorageService(
            @Value("${storage.s3.endpoint:}") String endpoint,
            @Value("${storage.s3.region:us-east-1}") String region,
            @Value("${storage.s3.accessKey:}") String accessKey,
            @Value("${storage.s3.secretKey:}") String secretKey,
            @Value("${storage.default-bucket:documents}") String defaultBucket
    ) {
        var b = S3Client.builder();
        if (endpoint != null && !endpoint.isBlank()) b.endpointOverride(URI.create(endpoint));
        b.region(Region.of(region));
        if (accessKey != null && !accessKey.isBlank()) {
            b.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        }
        this.s3 = b.build();
        this.defaultBucket = defaultBucket;
        this.endpoint = endpoint;
    }

    @Override
    public String upload(String bucket, String objectKey, InputStream data, long length, String contentType) throws Exception {
        if (bucket == null || bucket.isBlank()) bucket = defaultBucket;
        PutObjectRequest por = PutObjectRequest.builder().bucket(bucket).key(objectKey).contentType(contentType).contentLength(length).build();
        s3.putObject(por, RequestBody.fromInputStream(data, length));
        return objectKey;
    }

    @Override
    public InputStream download(String bucket, String objectKey) throws Exception {
        if (bucket == null || bucket.isBlank()) bucket = defaultBucket;
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }

    @Override
    public void delete(String bucket, String objectKey) throws Exception {
        if (bucket == null || bucket.isBlank()) bucket = defaultBucket;
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }

    @Override
    public String generatePresignedUrl(String bucket, String objectKey, int expiresInSeconds) throws Exception {
        if (bucket == null || bucket.isBlank()) bucket = defaultBucket;
        // If a custom endpoint is configured (e.g., MinIO or custom S3-compatible), construct a URL from it.
        if (endpoint != null && !endpoint.isBlank()) {
            String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length()-1) : endpoint;
            return String.format("%s/%s/%s", base, bucket, objectKey);
        }
        // Fallback to AWS S3 public URL pattern (not presigned): https://{bucket}.s3.amazonaws.com/{key}
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, objectKey);
    }
}

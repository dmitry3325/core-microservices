package com.corems.documentms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.documentms.app.config.StorageConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Service
public class S3StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageConfig storageConfig;

    public S3StorageService(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;

        String endpoint = storageConfig.getS3().getEndpoint();
        Region region = Region.of(storageConfig.getS3().getRegion());
        String accessKey = storageConfig.getS3().getAccessKey();
        String secretKey = storageConfig.getS3().getSecretKey();

        var clientBuilder = S3Client.builder();
        var presignerBuilder = S3Presigner.builder();

        if (endpoint != null && !endpoint.isBlank()) {
            URI endpointUri = URI.create(endpoint);
            clientBuilder.endpointOverride(endpointUri);
            presignerBuilder.endpointOverride(endpointUri);
        }

        clientBuilder.region(region);
        presignerBuilder.region(region);

        if (accessKey != null && !accessKey.isBlank()) {
            StaticCredentialsProvider credentialsProvider =
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
            clientBuilder.credentialsProvider(credentialsProvider);
            presignerBuilder.credentialsProvider(credentialsProvider);
        }

        this.s3 = clientBuilder.build();
        this.presigner = presignerBuilder.build();
    }

    public String upload(String bucket, String objectKey, InputStream data, long length, String contentType) {
        if (bucket == null || bucket.isBlank()) bucket = storageConfig.getDefaultBucket();

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(length)
                    .build();
            s3.putObject(request, RequestBody.fromInputStream(data, length));
            return objectKey;
        } catch (S3Exception e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                    "Failed to upload to S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                    "Failed to upload to S3: " + e.getMessage());
        }
    }

    public InputStream download(String bucket, String objectKey) {
        if (bucket == null || bucket.isBlank()) bucket = storageConfig.getDefaultBucket();

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            return s3.getObject(request);
        } catch (NoSuchKeyException e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST,
                    "Document not found in storage");
        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                throw ServiceException.of(DefaultExceptionReasonCodes.FORBIDDEN,
                        "Access denied to document storage");
            }
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                    "Failed to download from S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                    "Failed to download from S3: " + e.getMessage());
        }
    }

    public void delete(String bucket, String objectKey) {
        if (bucket == null || bucket.isBlank()) bucket = storageConfig.getDefaultBucket();

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            s3.deleteObject(request);
        } catch (S3Exception e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                    "Failed to delete from S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                    "Failed to delete from S3: " + e.getMessage());
        }
    }

    public String generatePresignedUrl(String bucket, String objectKey, int expiresInSeconds) {
        if (bucket == null || bucket.isBlank()) bucket = storageConfig.getDefaultBucket();

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expiresInSeconds))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (S3Exception e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                    "Failed to generate presigned URL: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR,
                    "Failed to generate presigned URL: " + e.getMessage());
        }
    }
}

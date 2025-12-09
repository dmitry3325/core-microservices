package com.corems.documentms.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for document storage and upload settings.
 * Binds to properties prefixed with 'storage' and 'app' in application configuration.
 */
@Configuration
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageConfig {

    /**
     * Default S3 bucket for document storage
     */
    private String defaultBucket = "documents";

    /**
     * S3 configuration
     */
    private S3Config s3 = new S3Config();

    @Getter
    @Setter
    public static class S3Config {
        private String endpoint;
        private String region = "us-east-1";
        private String accessKey;
        private String secretKey;
    }
}


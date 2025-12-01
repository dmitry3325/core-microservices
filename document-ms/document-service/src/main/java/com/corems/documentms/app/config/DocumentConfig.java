package com.corems.documentms.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration properties for application-level document settings.
 * Binds to properties prefixed with 'app' in application configuration.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class DocumentConfig {

    /**
     * Maximum upload file size in bytes (default: 100MB)
     */
    private long maxUploadSize = 104857600L;

    /**
     * Comma-separated list of allowed file extensions
     */
    private String allowedExtensions = "pdf,docx,doc,txt,jpg,jpeg,png,gif,xlsx,xls,csv,zip";

    /**
     * Buffer size for streaming operations in bytes (default: 8KB)
     */
    private StreamConfig stream = new StreamConfig();

    /**
     * Get allowed extensions as a Set
     */
    public Set<String> getAllowedExtensionsSet() {
        return Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Getter
    @Setter
    public static class StreamConfig {
        private int bufferSize = 8192;
    }
}


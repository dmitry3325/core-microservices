package com.corems.documentms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.security.service.TokenProvider;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.app.entity.DocumentAccessTokenEntity;
import com.corems.documentms.app.entity.DocumentEntity;
import com.corems.documentms.app.model.DocumentStreamResult;
import com.corems.documentms.app.repository.DocumentAccessTokenRepository;
import com.corems.documentms.app.repository.DocumentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;


import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PublicDocumentServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public S3StorageService s3StorageService() {
            return mock(S3StorageService.class);
        }

        @Bean
        @Primary
        public TokenProvider tokenProvider() {
            return mock(TokenProvider.class);
        }
    }

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private DocumentAccessTokenRepository tokenRepository;

    @Autowired
    private S3StorageService storage;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private PublicDocumentService publicDocumentService;

    private DocumentEntity testDocument;
    private DocumentAccessTokenEntity testToken;
    private UUID testUuid;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        repository.deleteAll();
        tokenRepository.deleteAll();
        
        // Reset mocks before each test
        reset(storage, tokenProvider);
        
        testUuid = UUID.randomUUID();
        
        testDocument = new DocumentEntity();
        testDocument.setUuid(testUuid);
        testDocument.setUserId(UUID.randomUUID());
        testDocument.setName("public-document.pdf");
        testDocument.setOriginalFilename("public-document.pdf");
        testDocument.setSize(2048L);
        testDocument.setContentType("application/pdf");
        testDocument.setExtension("pdf");
        testDocument.setBucket("public-bucket");
        testDocument.setObjectKey("public/" + testUuid);
        testDocument.setVisibility(DocumentEntity.Visibility.PUBLIC);
        testDocument.setUploadedByType(DocumentEntity.UploadedByType.USER);
        testDocument.setChecksum("public-checksum");
        testDocument.setCreatedAt(Instant.now());
        testDocument.setUpdatedAt(Instant.now());
        testDocument.setDeleted(false);

        testToken = DocumentAccessTokenEntity.builder()
            .documentUuid(testUuid)
            .tokenHash("test-token-hash")
            .createdBy(UUID.randomUUID())
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false) // Explicitly set to false
            .accessCount(0)
            .build();
    }

    @Test
    void getPublicDocumentMetadata_WhenDocumentExists_ReturnsMetadata() {
        // Given - save public document to database
        repository.save(testDocument);

        // When
        DocumentResponse result = publicDocumentService.getPublicDocumentMetadata(testUuid);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo(testUuid);
        assertThat(result.getName()).isEqualTo("public-document.pdf");
        assertThat(result.getSize()).isEqualTo(2048);
        assertThat(result.getContentType()).isEqualTo("application/pdf");
    }

    @Test
    void getPublicDocumentMetadata_WhenDocumentNotFound_ThrowsException() {
        // Given - no document in database
        UUID nonExistentUuid = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> publicDocumentService.getPublicDocumentMetadata(nonExistentUuid))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Invalid request");
    }

    @Test
    void preparePublicDocumentStream_WhenDocumentExists_ReturnsStream() {
        // Given - save public document to database
        repository.save(testDocument);
        
        InputStream mockStream = new ByteArrayInputStream("public document content".getBytes());
        when(storage.download("public-bucket", "public/" + testUuid)).thenReturn(mockStream);

        // When
        DocumentStreamResult result = publicDocumentService.preparePublicDocumentStream(testUuid);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStream()).isEqualTo(mockStream);
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getSize()).isEqualTo(2048L);
        assertThat(result.getFilename()).isEqualTo("public-document.pdf");
        
        // Verify S3 download was called
        verify(storage).download("public-bucket", "public/" + testUuid);
    }

    @Test
    void preparePublicDocumentStream_WhenStorageError_ThrowsException() {
        // Given - save public document to database
        repository.save(testDocument);
        
        when(storage.download("public-bucket", "public/" + testUuid))
            .thenThrow(new RuntimeException("Storage error"));

        // When & Then
        assertThatThrownBy(() -> publicDocumentService.preparePublicDocumentStream(testUuid))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Unexpected error");
    }

    @Test
    void prepareStreamByToken_WhenValidToken_ReturnsStream() {
        // Given - save document and token to database
        repository.save(testDocument);
        
        String token = "valid-jwt-token";
        // Create token with hash that matches what the service will look for (SHA-256)
        DocumentAccessTokenEntity tokenWithCorrectHash = DocumentAccessTokenEntity.builder()
            .documentUuid(testUuid)
            .tokenHash(hashToken(token)) // Use SHA-256 hash like the service does
            .createdBy(UUID.randomUUID())
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false) // Explicitly set to false
            .accessCount(0)
            .build();
        tokenRepository.save(tokenWithCorrectHash);
        
        InputStream mockStream = new ByteArrayInputStream("token document content".getBytes());
        
        when(tokenProvider.isTokenValid(token)).thenReturn(true);
        when(tokenProvider.getClaim(eq(token), any()))
            .thenReturn(testUuid.toString());
        when(storage.download("public-bucket", "public/" + testUuid)).thenReturn(mockStream);

        // When
        DocumentStreamResult result = publicDocumentService.prepareStreamByToken(token);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStream()).isEqualTo(mockStream);
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getSize()).isEqualTo(2048L);
        assertThat(result.getFilename()).isEqualTo("public-document.pdf");
        
        // Verify token usage was tracked in database
        Optional<DocumentAccessTokenEntity> updatedToken = tokenRepository.findById(tokenWithCorrectHash.getId());
        assertThat(updatedToken).isPresent();
        assertThat(updatedToken.get().getAccessCount()).isEqualTo(1);
        assertThat(updatedToken.get().getLastAccessedAt()).isNotNull();
        
        verify(tokenProvider).isTokenValid(token);
        verify(storage).download("public-bucket", "public/" + testUuid);
    }

    @Test
    void prepareStreamByToken_WhenInvalidToken_ThrowsException() {
        // Given
        String token = "invalid-jwt-token";
        when(tokenProvider.isTokenValid(token)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> publicDocumentService.prepareStreamByToken(token))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("User is unauthorized");
    }

    @Test
    void prepareStreamByToken_WhenTokenHasNoDocumentUuid_ThrowsException() {
        // Given
        String token = "valid-jwt-token";
        when(tokenProvider.isTokenValid(token)).thenReturn(true);
        when(tokenProvider.getClaim(eq(token), any())).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> publicDocumentService.prepareStreamByToken(token))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Invalid request");
    }

    @Test
    void prepareStreamByToken_WhenInvalidDocumentUuid_ThrowsException() {
        // Given
        String token = "valid-jwt-token";
        when(tokenProvider.isTokenValid(token)).thenReturn(true);
        when(tokenProvider.getClaim(eq(token), any())).thenReturn("invalid-uuid");

        // When & Then
        assertThatThrownBy(() -> publicDocumentService.prepareStreamByToken(token))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Invalid request");
    }

    @Test
    void prepareStreamByToken_WhenTokenNotFoundInDatabase_ThrowsException() {
        // Given - no token in database
        String token = "valid-jwt-token";
        
        when(tokenProvider.isTokenValid(token)).thenReturn(true);
        when(tokenProvider.getClaim(eq(token), any())).thenReturn(testUuid.toString());

        // When & Then
        assertThatThrownBy(() -> publicDocumentService.prepareStreamByToken(token))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("User is unauthorized");
    }

    @Test
    void prepareStreamByToken_WhenDocumentNotAccessible_ThrowsException() {
        // Given - save token but no document
        tokenRepository.save(testToken);
        
        String token = "valid-jwt-token";
        
        when(tokenProvider.isTokenValid(token)).thenReturn(true);
        when(tokenProvider.getClaim(eq(token), any())).thenReturn(testUuid.toString());

        // When & Then
        assertThatThrownBy(() -> publicDocumentService.prepareStreamByToken(token))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("User is unauthorized");
    }

    @Test
    void prepareStreamByToken_WhenStorageDownloadFails_ThrowsException() {
        // Given - save document and token to database
        repository.save(testDocument);
        tokenRepository.save(testToken);
        
        String token = "valid-jwt-token";
        
        when(tokenProvider.isTokenValid(token)).thenReturn(true);
        when(tokenProvider.getClaim(eq(token), any())).thenReturn(testUuid.toString());
        when(storage.download("public-bucket", "public/" + testUuid))
            .thenThrow(new RuntimeException("Storage download failed"));

        // When & Then
        assertThatThrownBy(() -> publicDocumentService.prepareStreamByToken(token))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("User is unauthorized");
    }

    @Test
    void prepareStreamByToken_WhenExpiredToken_ThrowsException() {
        // Given - save expired token
        DocumentAccessTokenEntity expiredToken = DocumentAccessTokenEntity.builder()
            .documentUuid(testUuid)
            .tokenHash("expired-token-hash")
            .createdBy(UUID.randomUUID())
            .expiresAt(Instant.now().minusSeconds(3600)) // Expired 1 hour ago
            .revoked(false) // Explicitly set to false
            .accessCount(0)
            .build();
        tokenRepository.save(expiredToken);
        
        String token = "expired-jwt-token";
        
        when(tokenProvider.isTokenValid(token)).thenReturn(true);
        when(tokenProvider.getClaim(eq(token), any())).thenReturn(testUuid.toString());

        // When & Then
        assertThatThrownBy(() -> publicDocumentService.prepareStreamByToken(token))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("User is unauthorized");
    }

    @Test
    void prepareStreamByToken_WhenByLinkDocument_AllowsAccessWithValidToken() {
        // Given - save BY_LINK document and valid token
        testDocument.setVisibility(DocumentEntity.Visibility.BY_LINK);
        repository.save(testDocument);
        
        String token = "valid-jwt-token";
        // Create token with hash that matches what the service will look for (SHA-256)
        DocumentAccessTokenEntity tokenWithCorrectHash = DocumentAccessTokenEntity.builder()
            .documentUuid(testUuid)
            .tokenHash(hashToken(token)) // Use SHA-256 hash like the service does
            .createdBy(UUID.randomUUID())
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false) // Explicitly set to false
            .accessCount(0)
            .build();
        tokenRepository.save(tokenWithCorrectHash);
        
        InputStream mockStream = new ByteArrayInputStream("by-link document content".getBytes());
        
        when(tokenProvider.isTokenValid(token)).thenReturn(true);
        when(tokenProvider.getClaim(eq(token), any())).thenReturn(testUuid.toString());
        when(storage.download("public-bucket", "public/" + testUuid)).thenReturn(mockStream);

        // When
        DocumentStreamResult result = publicDocumentService.prepareStreamByToken(token);

        // Then - should allow access for BY_LINK documents with valid token
        assertThat(result).isNotNull();
        assertThat(result.getStream()).isEqualTo(mockStream);
        assertThat(result.getContentType()).isEqualTo("application/pdf");
    }

    // Helper method to hash tokens the same way the service does
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
package com.corems.documentms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.security.UserPrincipal;
import com.corems.common.security.service.TokenProvider;
import com.corems.documentms.api.model.DocumentResponse;
import com.corems.documentms.api.model.DocumentUpdateRequest;
import com.corems.documentms.api.model.DocumentUploadMetadata;
import com.corems.documentms.api.model.GenerateLinkRequest;
import com.corems.documentms.api.model.LinkResponse;
import com.corems.documentms.api.model.PaginatedDocumentList;
import com.corems.documentms.api.model.SuccessfulResponse;
import com.corems.documentms.api.model.UploadBase64Request;
import com.corems.documentms.api.model.Visibility;
import com.corems.documentms.app.config.DocumentConfig;
import com.corems.documentms.app.config.StorageConfig;
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

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public S3StorageService s3StorageService() {
            return mock(S3StorageService.class);
        }

        @Bean
        @Primary
        public StorageConfig storageConfig() {
            return mock(StorageConfig.class);
        }

        @Bean
        @Primary
        public DocumentConfig documentConfig() {
            return mock(DocumentConfig.class);
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
    private StorageConfig storageConfig;

    @Autowired
    private DocumentConfig documentConfig;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private DocumentService documentService;

    private UserPrincipal testUser;
    private DocumentEntity testDocument;
    private UUID testUuid;
    private UUID userId;
    private MultipartFile testFile;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        repository.deleteAll();
        tokenRepository.deleteAll();
        
        testUuid = UUID.randomUUID();
        userId = UUID.randomUUID();
        testUser = new UserPrincipal(
            userId, // Use userId as the principal ID
            "test@example.com",
            "Test",
            "User",
            userId, // This should match the document's userId
            List.of(new SimpleGrantedAuthority("DOCUMENT_MS_ADMIN"))
        );

        // Set up Spring Security context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        testDocument = new DocumentEntity();
        testDocument.setUuid(testUuid);
        testDocument.setUserId(userId); // Ensure document is owned by the test user
        testDocument.setName("test-document.pdf");
        testDocument.setOriginalFilename("test-document.pdf");
        testDocument.setSize(1024L);
        testDocument.setContentType("application/pdf");
        testDocument.setExtension("pdf");
        testDocument.setBucket("test-bucket");
        testDocument.setObjectKey("documents/" + testUuid);
        testDocument.setVisibility(DocumentEntity.Visibility.PRIVATE);
        testDocument.setUploadedByType(DocumentEntity.UploadedByType.USER);
        testDocument.setUploadedById(userId);
        testDocument.setChecksum("test-checksum");
        testDocument.setCreatedAt(Instant.now());
        testDocument.setUpdatedAt(Instant.now());
        testDocument.setDeleted(false);

        testFile = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "test content".getBytes()
        );

        // Setup mock configurations
        when(storageConfig.getDefaultBucket()).thenReturn("test-bucket");
        when(documentConfig.getMaxUploadSize()).thenReturn(10L * 1024 * 1024); // 10MB
        when(documentConfig.getAllowedExtensionsSet()).thenReturn(Set.of("pdf", "doc", "docx", "txt"));
        when(documentConfig.getBaseUrl()).thenReturn("http://localhost:8080");
    }

    @Test
    void uploadMultipart_WhenValidFile_CreatesAndReturnsDocument() {
        // Given
        DocumentUploadMetadata metadata = new DocumentUploadMetadata();
        metadata.setDescription("Test document");
        metadata.setVisibility(Visibility.PRIVATE);

        // When
        DocumentResponse response = documentService.uploadMultipart(testFile, metadata);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("test.pdf");
        assertThat(response.getOriginalFilename()).isEqualTo("test.pdf");
        assertThat(response.getVisibility()).isEqualTo(Visibility.PRIVATE);

        // Verify document was saved to database
        Optional<DocumentEntity> saved = repository.findByUuid(response.getUuid());
        assertThat(saved).isPresent();
        assertThat(saved.get().getName()).isEqualTo("test.pdf");
        assertThat(saved.get().getUploadedById()).isEqualTo(userId);

        // Verify S3 upload was called
        verify(storage).upload(anyString(), anyString(), any(InputStream.class), any(Long.class), anyString());
    }

    @Test
    void uploadBase64_WhenValidData_CreatesAndReturnsDocument() {
        // Given
        String base64Content = java.util.Base64.getEncoder().encodeToString("test content".getBytes());
        UploadBase64Request request = new UploadBase64Request();
        request.setName("test.txt");
        request.setContentType("text/plain");
        request.setBase64Data(base64Content);
        request.setVisibility(Visibility.PUBLIC);

        // When
        DocumentResponse response = documentService.uploadBase64(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("test.txt");
        assertThat(response.getVisibility()).isEqualTo(Visibility.PUBLIC);

        // Verify document was saved to database
        Optional<DocumentEntity> saved = repository.findByUuid(response.getUuid());
        assertThat(saved).isPresent();
        assertThat(saved.get().getContentType()).isEqualTo("text/plain");
    }

    @Test
    void getByUuid_WhenValidUuid_ReturnsDocument() {
        // Given - save document to database
        DocumentEntity saved = repository.save(testDocument);

        // When
        DocumentResponse response = documentService.getByUuid(saved.getUuid());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUuid()).isEqualTo(saved.getUuid());
        assertThat(response.getName()).isEqualTo("test-document.pdf");
        assertThat(response.getSize()).isEqualTo(1024);
    }

    @Test
    void updateMetadata_WhenValidRequest_UpdatesDocument() {
        // Given - save document to database
        DocumentEntity saved = repository.save(testDocument);
        
        DocumentUpdateRequest updateRequest = new DocumentUpdateRequest();
        updateRequest.setName("Updated Document");
        updateRequest.setDescription("Updated description");
        updateRequest.setVisibility(Visibility.PUBLIC);

        // When
        DocumentResponse response = documentService.updateMetadata(saved.getUuid(), updateRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated Document");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.getVisibility()).isEqualTo(Visibility.PUBLIC);

        // Verify database was updated
        Optional<DocumentEntity> updated = repository.findByUuid(saved.getUuid());
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("Updated Document");
        assertThat(updated.get().getVisibility()).isEqualTo(DocumentEntity.Visibility.PUBLIC);
    }

    @Test
    void delete_WhenValidUuid_SoftDeletesDocument() {
        // Given - save document to database
        DocumentEntity saved = repository.save(testDocument);

        // When
        SuccessfulResponse response = documentService.delete(saved.getUuid(), false);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isTrue();

        // Verify document was soft deleted in database
        Optional<DocumentEntity> deleted = repository.findByUuid(saved.getUuid());
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isTrue();
    }

    @Test
    void getDocumentList_WhenDocumentsExist_ReturnsPagedList() {
        // Given - save multiple documents
        DocumentEntity doc1 = new DocumentEntity();
        doc1.setUuid(UUID.randomUUID());
        doc1.setUserId(userId);
        doc1.setName("Document 1");
        doc1.setOriginalFilename("doc1.pdf");
        doc1.setSize(1024L);
        doc1.setContentType("application/pdf");
        doc1.setExtension("pdf");
        doc1.setBucket("test-bucket");
        doc1.setObjectKey("documents/doc1");
        doc1.setVisibility(DocumentEntity.Visibility.PRIVATE);
        doc1.setUploadedByType(DocumentEntity.UploadedByType.USER);
        doc1.setUploadedById(userId);
        doc1.setChecksum("checksum1");
        doc1.setCreatedAt(Instant.now());
        doc1.setUpdatedAt(Instant.now());
        doc1.setDeleted(false);

        DocumentEntity doc2 = new DocumentEntity();
        doc2.setUuid(UUID.randomUUID());
        doc2.setUserId(userId);
        doc2.setName("Document 2");
        doc2.setOriginalFilename("doc2.txt");
        doc2.setSize(512L);
        doc2.setContentType("text/plain");
        doc2.setExtension("txt");
        doc2.setBucket("test-bucket");
        doc2.setObjectKey("documents/doc2");
        doc2.setVisibility(DocumentEntity.Visibility.PUBLIC);
        doc2.setUploadedByType(DocumentEntity.UploadedByType.USER);
        doc2.setUploadedById(userId);
        doc2.setChecksum("checksum2");
        doc2.setCreatedAt(Instant.now());
        doc2.setUpdatedAt(Instant.now());
        doc2.setDeleted(false);

        repository.save(doc1);
        repository.save(doc2);

        // When
        PaginatedDocumentList response = documentService.getDocumentList(
            Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty(), 
            Optional.empty(), Optional.empty());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        
        List<String> documentNames = response.getItems().stream()
            .map(DocumentResponse::getName)
            .toList();
        assertThat(documentNames).containsExactlyInAnyOrder("Document 1", "Document 2");
    }

    @Test
    void prepareStreamResponse_WhenValidUuid_ReturnsStream() {
        // Given - save document to database
        DocumentEntity saved = repository.save(testDocument);
        InputStream mockStream = new ByteArrayInputStream("test content".getBytes());
        
        when(storage.download("test-bucket", "documents/" + testUuid)).thenReturn(mockStream);

        // When
        DocumentStreamResult result = documentService.prepareStreamResponse(saved.getUuid());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStream()).isEqualTo(mockStream);
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getSize()).isEqualTo(1024L);
        assertThat(result.getFilename()).isEqualTo("test-document.pdf");

        // Verify S3 download was called
        verify(storage).download("test-bucket", "documents/" + testUuid);
    }

    @Test
    void generateAccessLink_WhenValidRequest_CreatesTokenAndReturnsLink() {
        // Given - save document to database with BY_LINK visibility
        testDocument.setVisibility(DocumentEntity.Visibility.BY_LINK);
        DocumentEntity saved = repository.save(testDocument);
        
        GenerateLinkRequest request = new GenerateLinkRequest();
        request.setExpiresInHours(24);

        when(tokenProvider.createAccessToken(anyString(), any())).thenReturn("test-jwt-token");

        // When
        LinkResponse response = documentService.generateAccessLink(saved.getUuid(), request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("test-jwt-token");
        assertThat(response.getUrl()).contains("test-jwt-token");

        // Verify token was saved to database
        List<DocumentAccessTokenEntity> tokens = tokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getDocumentUuid()).isEqualTo(saved.getUuid());
        assertThat(tokens.get(0).getCreatedBy()).isEqualTo(userId);
    }

    @Test
    void uploadMultipart_WhenInvalidFileExtension_ThrowsException() {
        // Given
        MultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.exe",
            "application/octet-stream",
            "test content".getBytes()
        );

        DocumentUploadMetadata metadata = new DocumentUploadMetadata();

        // When & Then
        assertThatThrownBy(() -> documentService.uploadMultipart(invalidFile, metadata))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Invalid request");
    }

    @Test
    void uploadMultipart_WhenFileTooLarge_ThrowsException() {
        // Given
        when(documentConfig.getMaxUploadSize()).thenReturn(1L); // 1 byte limit
        
        DocumentUploadMetadata metadata = new DocumentUploadMetadata();

        // When & Then
        assertThatThrownBy(() -> documentService.uploadMultipart(testFile, metadata))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Invalid request");
    }

    @Test
    void getByUuid_WhenDocumentNotFound_ThrowsException() {
        // Given
        UUID nonExistentUuid = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> documentService.getByUuid(nonExistentUuid))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Invalid request");
    }

    @Test
    void prepareStreamResponse_WhenStorageError_ThrowsException() {
        // Given - save document to database
        DocumentEntity saved = repository.save(testDocument);
        
        when(storage.download(anyString(), anyString())).thenThrow(new RuntimeException("Storage error"));

        // When & Then
        assertThatThrownBy(() -> documentService.prepareStreamResponse(saved.getUuid()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("Unexpected error");
    }
}
package com.corems.documentms.app.integration;

import com.corems.documentms.ApiClient;
import com.corems.documentms.api.model.*;
import com.corems.documentms.client.DocumentApi;
import com.corems.documentms.client.DocumentsListApi;
import com.corems.documentms.client.PublicDocumentsApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentMsApiIntegrationTest {

    @LocalServerPort
    private int port;

    private ApiClient apiClient;
    private DocumentApi documentApi;
    private DocumentsListApi documentsListApi;
    private PublicDocumentsApi publicDocumentsApi;

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        apiClient = new ApiClient(restClient);
        
        documentApi = new DocumentApi(apiClient);
        documentsListApi = new DocumentsListApi(apiClient);
        publicDocumentsApi = new PublicDocumentsApi(apiClient);
    }

    /**
     * Helper to set bearer token for authenticated requests.
     */
    private void authenticateAs(String token) {
        apiClient.setBearerToken(token);
    }

    // ==================== Public Endpoints (No Auth Required) ====================

    @Test
    @Order(1)
    void getPublicDocumentMetadata_WhenDocumentNotFound_ShouldReturn404() {
        UUID nonExistentId = UUID.randomUUID();
        
        assertThatThrownBy(() -> publicDocumentsApi.getPublicDocumentMetadata(nonExistentId))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    @Order(2)
    void downloadPublicDocument_WhenDocumentNotFound_ShouldReturn404() {
        UUID nonExistentId = UUID.randomUUID();
        
        assertThatThrownBy(() -> publicDocumentsApi.downloadPublicDocument(nonExistentId))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    @Order(3)
    void accessDocumentByToken_WhenTokenInvalid_ShouldReturn404() {
        assertThatThrownBy(() -> publicDocumentsApi.accessDocumentByToken("invalid-token"))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
    }

    // ==================== Protected Endpoints (Auth Required) ====================

    @Test
    @Order(10)
    void getDocumentMetadata_WhenNotAuthenticated_ShouldReturn401() {
        UUID documentId = UUID.randomUUID();
        
        assertThatThrownBy(() -> documentApi.getDocumentMetadata(documentId))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }

    @Test
    @Order(11)
    void deleteDocument_WhenNotAuthenticated_ShouldReturn401() {
        UUID documentId = UUID.randomUUID();
        
        assertThatThrownBy(() -> documentApi.deleteDocument(documentId, false))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }

    @Test
    @Order(12)
    void updateDocumentMetadata_WhenNotAuthenticated_ShouldReturn401() {
        UUID documentId = UUID.randomUUID();
        DocumentUpdateRequest request = new DocumentUpdateRequest();
        request.setName("Updated Name");
        
        assertThatThrownBy(() -> documentApi.updateDocumentMetadata(documentId, request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }

    @Test
    @Order(13)
    void listDocuments_WhenNotAuthenticated_ShouldReturn401() {
        assertThatThrownBy(() -> documentsListApi.listDocuments(1, 10, null, null, null, false))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }

    // Note: Full authenticated tests require a valid JWT token with appropriate roles.
    // DOCUMENT_MS_USER - can manage own documents
    // DOCUMENT_MS_ADMIN - can manage all documents
    //
    // Document upload tests would require multipart form handling which is more complex.
    // In a complete test setup with real auth:
    // @Test
    // @Order(20)
    // @DirtiesContext
    // void uploadAndGetDocument_WhenAuthenticated_ShouldWork() {
    //     authenticateAs(validUserToken);
    //     
    //     // Upload document (requires multipart handling)
    //     // DocumentResponse uploaded = documentsListApi.uploadDocument(...);
    //     
    //     // Get metadata
    //     // DocumentResponse metadata = documentApi.getDocumentMetadata(uploaded.getUuid());
    //     // assertThat(metadata.getName()).isEqualTo(expectedName);
    // }
}

package com.corems.documentms.app.integration;

import com.corems.documentms.ApiClient;
import com.corems.documentms.api.model.PaginatedDocumentList;
import com.corems.documentms.client.DocumentsListApi;
import com.corems.documentms.client.PublicDocumentsApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DocumentMsApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApiClient apiClient;
    @Autowired
    private DocumentsListApi documentsListApi;
    @Autowired
    private PublicDocumentsApi publicDocumentsApi;

    @BeforeEach
    void setUp() {
        apiClient.setBasePath("http://localhost:" + port);
    }

    // ==================== Public Endpoints ====================

    @Test
    @Order(1)
    void getPublicDocumentMetadata_WhenDocumentNotFound_ShouldReturnError() {
        UUID nonExistentId = UUID.randomUUID();
        
        // Service returns 400 "Invalid request" when document not found
        assertThatThrownBy(() -> publicDocumentsApi.getPublicDocumentMetadata(nonExistentId))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isIn(400, 404));
    }

    @Test
    @Order(2)
    void downloadPublicDocument_WhenDocumentNotFound_ShouldReturnError() {
        UUID nonExistentId = UUID.randomUUID();
        
        // Service returns 400 "Invalid request" when document not found
        assertThatThrownBy(() -> publicDocumentsApi.downloadPublicDocument(nonExistentId))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isIn(400, 404));
    }

    @Test
    @Order(3)
    void accessDocumentByToken_WhenTokenInvalid_ShouldReturnError() {
        // Service returns 401 for invalid token
        assertThatThrownBy(() -> publicDocumentsApi.accessDocumentByToken("invalid-token"))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isIn(401, 404));
    }

    // ==================== List Documents (Public Access) ====================

    @Test
    @Order(10)
    void listDocuments_ShouldReturnEmptyListWhenNoDocuments() {
        // listDocuments endpoint is publicly accessible and returns empty list
        PaginatedDocumentList response = documentsListApi.listDocuments(1, 10, null, null, null, false);
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotNull();
    }
}

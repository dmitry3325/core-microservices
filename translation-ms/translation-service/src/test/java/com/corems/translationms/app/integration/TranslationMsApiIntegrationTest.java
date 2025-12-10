package com.corems.translationms.app.integration;

import com.corems.translationms.ApiClient;
import com.corems.translationms.client.TranslationApi;
import com.corems.translationms.client.TranslationAdminApi;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TranslationMsApiIntegrationTest {

    @LocalServerPort
    private int port;

    private ApiClient apiClient;
    private TranslationApi translationApi;
    private TranslationAdminApi translationAdminApi;

    private static final String TEST_REALM = "test-realm";
    private static final String TEST_LANG = "en";

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        apiClient = new ApiClient(restClient);
        
        translationApi = new TranslationApi(apiClient);
        translationAdminApi = new TranslationAdminApi(apiClient);
    }

    /**
     * Helper to set bearer token for authenticated requests.
     * In a real scenario, this would obtain a token from user-ms.
     * For testing, we use a mock/test token.
     */
    private void authenticateAsAdmin(String token) {
        apiClient.setBearerToken(token);
    }

    // ==================== Public Endpoints (No Auth Required) ====================

    @Test
    @Order(1)
    void getAvailableLanguages_WhenRealmNotFound_ShouldReturn404() {
        assertThatThrownBy(() -> translationApi.getAvailableLanguagesByRealm("non-existent-realm"))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    @Order(2)
    void getTranslation_WhenRealmOrLangNotFound_ShouldReturn404() {
        assertThatThrownBy(() -> translationApi.getTranslationByRealmAndLang("non-existent", "en"))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
    }

    // ==================== Admin Endpoints (Auth Required) ====================

    @Test
    @Order(10)
    void adminEndpoints_WhenNotAuthenticated_ShouldReturn401() {
        // No token set
        assertThatThrownBy(() -> translationAdminApi.realmsList(1, 10, null, null))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));

        assertThatThrownBy(() -> translationAdminApi.getTranslationAdminByRealmAndLang(TEST_REALM, TEST_LANG))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }

    // Note: Full admin endpoint tests require a valid JWT token with TRANSLATION_MS_ADMIN role.
    // In a complete test setup, you would:
    // 1. Either mock the security context
    // 2. Or obtain a real token from user-ms (requires user-ms to be running)
    // 3. Or use @WithMockUser with MockMvc (not with API clients)
    
    // Example of how authenticated tests would look:
    // @Test
    // @Order(11)
    // @DirtiesContext
    // void createAndGetTranslation_WhenAuthenticated_ShouldWork() {
    //     authenticateAsAdmin(validAdminToken);
    //     
    //     TranslationUpdateRequest request = new TranslationUpdateRequest();
    //     request.setTranslations(Map.of("greeting", "Hello", "farewell", "Goodbye"));
    //     
    //     SuccessfulResponse response = translationAdminApi.updateTranslationAdminByRealmAndLang(
    //         TEST_REALM, TEST_LANG, request);
    //     assertThat(response.getResult()).isTrue();
    //     
    //     // Verify via public API
    //     Map<String, String> translations = translationApi.getTranslationByRealmAndLang(TEST_REALM, TEST_LANG);
    //     assertThat(translations).containsEntry("greeting", "Hello");
    // }
}

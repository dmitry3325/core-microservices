package com.corems.translationms.app.integration;

import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.service.TokenProvider;
import com.corems.translationms.ApiClient;
import com.corems.translationms.client.TranslationApi;
import com.corems.translationms.client.TranslationAdminApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TranslationMsApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private TranslationApi translationApi;
    @Autowired
    private TranslationAdminApi translationAdminApi;

    private static final String TEST_TOKEN_ID = UUID.randomUUID().toString();
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_ADMIN_ID = UUID.randomUUID();
    private static final String TEST_USER_EMAIL = "testuser@example.com";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final String TEST_REALM = "test-realm";
    private static final String TEST_LANG = "en";

    @BeforeEach
    void setUp() {
        apiClient.setBasePath("http://localhost:" + port);
        authenticateAsAdmin();
    }

    private String createToken(UUID userId, String email, List<String> roles) {
        Map<String, Object> claims = Map.of(
            TokenProvider.CLAIM_USER_ID, userId.toString(),
            TokenProvider.CLAIM_EMAIL, email,
            TokenProvider.CLAIM_FIRST_NAME, "Test",
            TokenProvider.CLAIM_LAST_NAME, "User",
            TokenProvider.CLAIM_ROLES, roles
        );
        return tokenProvider.createAccessToken(TEST_TOKEN_ID, claims);
    }

    private void authenticateAsUser() {
        String token = createToken(TEST_USER_ID, TEST_USER_EMAIL, 
            List.of(CoreMsRoles.USER_MS_USER.name()));
        apiClient.setBearerToken(token);
    }

    private void authenticateAsAdmin() {
        String token = createToken(TEST_ADMIN_ID, TEST_ADMIN_EMAIL, 
            List.of(CoreMsRoles.TRANSLATION_MS_ADMIN.name()));
        apiClient.setBearerToken(token);
    }

    @Test
    @Order(1)
    void getAvailableLanguages_WhenRealmNotFound_ShouldReturnEmptyOrNotFound() {
        authenticateAsAdmin();
        // API may return empty list or 404 for non-existent realm
        try {
            var result = translationApi.getAvailableLanguagesByRealm("non-existent-realm");
            assertThat(result).isEmpty();
        } catch (RestClientResponseException ex) {
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Test
    @Order(2)
    void getTranslation_WhenRealmOrLangNotFound_ShouldReturnEmptyOrNotFound() {
        authenticateAsAdmin();
        // API may return empty object or 404 for non-existent realm/lang
        try {
            var result = translationApi.getTranslationByRealmAndLang("non-existent", "en");
            assertThat(result).isNotNull();
        } catch (RestClientResponseException ex) {
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
        }
    }

    // ==================== Admin Endpoints (Auth Required) ====================

    @Test
    @Order(10)
    void adminEndpoints_WhenNotAuthenticated_ShouldReturn403() {
        authenticateAsUser();
        // No token set - Spring Security returns 403 for unauthenticated requests
        assertThatThrownBy(() -> translationAdminApi.realmsList(1, 10, null, null))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(403));

        assertThatThrownBy(() -> translationAdminApi.getTranslationAdminByRealmAndLang(TEST_REALM, TEST_LANG))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @Order(11)
    void adminEndpoints_WhenAuthenticated_ShouldBeAccessible() {
        authenticateAsAdmin();
        // realmsList should work even if empty
        assertThat(translationAdminApi.realmsList(1, 10, null, null)).isNotNull();
        
        // getTranslationAdminByRealmAndLang may return 404 if realm doesn't exist
        try {
            var result = translationAdminApi.getTranslationAdminByRealmAndLang(TEST_REALM, TEST_LANG);
            assertThat(result).isNotNull();
        } catch (RestClientResponseException ex) {
            // 404 is acceptable if the realm doesn't exist
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
        }
    }
}

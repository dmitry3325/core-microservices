package com.corems.communicationms.app.integration;

import com.corems.communicationms.ApiClient;
import com.corems.communicationms.api.model.*;
import com.corems.communicationms.client.MessagesApi;
import com.corems.communicationms.client.NotificationsApi;
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
class CommunicationMsApiIntegrationTest {

    @LocalServerPort
    private int port;

    private ApiClient apiClient;
    private MessagesApi messagesApi;
    private NotificationsApi notificationsApi;

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        apiClient = new ApiClient(restClient);
        
        messagesApi = new MessagesApi(apiClient);
        notificationsApi = new NotificationsApi(apiClient);
    }

    /**
     * Helper to set bearer token for authenticated requests.
     */
    private void authenticateAs(String token) {
        apiClient.setBearerToken(token);
    }

    // ==================== Unauthorized Access Tests ====================

    @Test
    @Order(1)
    void getMessages_WhenNotAuthenticated_ShouldReturn401() {
        assertThatThrownBy(() -> messagesApi.getMessages(1, 10, null, null, null))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }

    @Test
    @Order(2)
    void sendEmailMessage_WhenNotAuthenticated_ShouldReturn401() {
        EmailMessageRequest request = new EmailMessageRequest();
        request.setUserId(UUID.randomUUID());
        request.setSubject("Test Subject");
        request.setBody("Test Body");

        assertThatThrownBy(() -> messagesApi.sendEmailMessage(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }

    @Test
    @Order(3)
    void sendSmsMessage_WhenNotAuthenticated_ShouldReturn401() {
        SmsMessageRequest request = new SmsMessageRequest();
        request.setUserId(UUID.randomUUID());
        request.setPhoneNumber("+15551234567");
        request.setMessage("Test SMS");

        assertThatThrownBy(() -> messagesApi.sendSmsMessage(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }

    @Test
    @Order(4)
    void sendEmailNotification_WhenNotAuthenticated_ShouldReturn401() {
        EmailNotificationRequest request = new EmailNotificationRequest();
        request.setRecipient("test@example.com");
        request.setSubject("Test Subject");
        request.setBody("Test Body");

        assertThatThrownBy(() -> notificationsApi.sendEmailNotification(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }

    // Note: Full authenticated tests require a valid JWT token with appropriate roles.
    // COMMUNICATION_MS_USER - can view own messages/notifications
    // COMMUNICATION_MS_ADMIN - can send messages and view all
    //
    // In a complete test setup with real auth:
    // @Test
    // @Order(10)
    // @DirtiesContext
    // void sendAndGetMessages_WhenAuthenticatedAsAdmin_ShouldWork() {
    //     authenticateAs(validAdminToken);
    //     
    //     EmailMessageRequest request = new EmailMessageRequest();
    //     request.setUserId(testUserId);
    //     request.setSubject("Test Subject");
    //     request.setBody("Test Body");
    //     
    //     MessageResponse response = messagesApi.sendEmailMessage(request);
    //     assertThat(response.getId()).isNotNull();
    //     
    //     MessageListResponse messages = messagesApi.getMessages(1, 10, null, null, null);
    //     assertThat(messages.getItems()).isNotEmpty();
    // }
}

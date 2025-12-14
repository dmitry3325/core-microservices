package com.corems.communicationms.app.integration;

import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.service.TokenProvider;
import com.corems.communicationms.ApiClient;
import com.corems.communicationms.api.model.*;
import com.corems.communicationms.client.MessagesApi;
import com.corems.communicationms.client.NotificationsApi;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CommunicationMsApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private MessagesApi messagesApi;
    @Autowired
    private NotificationsApi notificationsApi;

    private static final String TEST_TOKEN_ID = UUID.randomUUID().toString();
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_ADMIN_ID = UUID.randomUUID();
    private static final String TEST_USER_EMAIL = "testuser@example.com";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";

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
            List.of(CoreMsRoles.COMMUNICATION_MS_USER.name()));
        apiClient.setBearerToken(token);
    }

    private void authenticateAsAdmin() {
        String token = createToken(TEST_ADMIN_ID, TEST_ADMIN_EMAIL, 
            List.of(CoreMsRoles.COMMUNICATION_MS_ADMIN.name()));
        apiClient.setBearerToken(token);
    }

    @BeforeEach
    void setUp() {
        apiClient.setBasePath("http://localhost:" + port);
        authenticateAsAdmin();
    }


    @Test
    @Order(1)
    void sendEmailMessage_WhenAuthenticatedAsUser_ShouldBeDenied() {
        authenticateAsUser();

        EmailMessageRequest request = new EmailMessageRequest();
        request.setUserId(UUID.randomUUID());
        request.setRecipient("recipient@example.com");
        request.setSubject("Test Subject");
        request.setBody("Test Body");

        assertThatThrownBy(() -> messagesApi.sendEmailMessage(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isIn(401, 403));
    }

    @Test
    @Order(2)
    void sendSmsMessage_WhenAuthenticatedAsUser_ShouldBeDenied() {
        authenticateAsUser();

        SmsMessageRequest request = new SmsMessageRequest();
        request.setUserId(UUID.randomUUID());
        request.setPhoneNumber("+15551234567");
        request.setMessage("Test SMS");

        assertThatThrownBy(() -> messagesApi.sendSmsMessage(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isIn(401, 403));
    }

    @Test
    @Order(3)
    void sendEmailNotification_WhenAuthenticatedAsUser_ShouldBeDenied() {
        authenticateAsUser();

        EmailNotificationRequest request = new EmailNotificationRequest();
        request.setRecipient("test@example.com");
        request.setSubject("Test Subject");
        request.setBody("Test Body");

        assertThatThrownBy(() -> notificationsApi.sendEmailNotification(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isIn(401, 403));
    }

    @Test
    @Order(4)
    void sendSmsNotification_WhenAuthenticatedAsUser_ShouldBeDenied() {
        authenticateAsUser();

        SmsNotificationRequest request = new SmsNotificationRequest();
        request.setPhoneNumber("+15551234567");
        request.setMessage("Test notification");

        assertThatThrownBy(() -> notificationsApi.sendSmsNotification(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isIn(401, 403));
    }

    @Test
    @Order(5)
    void sendSlackNotification_WhenAuthenticatedAsUser_ShouldBeDenied() {
        authenticateAsUser();

        SlackNotificationRequest request = new SlackNotificationRequest();
        request.setChannel("#general");
        request.setMessage("Test slack notification");

        assertThatThrownBy(() -> notificationsApi.sendSlackNotification(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isIn(401, 403));
    }

    // ==================== Admin - Messages Functionality ====================

    @Test
    @Order(10)
    void sendEmailMessage_ShouldCreateMessage() {
        EmailMessageRequest request = new EmailMessageRequest();
        request.setUserId(TEST_USER_ID);
        request.setRecipient("recipient@example.com");
        request.setSubject("Test Email Subject");
        request.setBody("Test email body content");

        MessageResponse response = messagesApi.sendEmailMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getUuid()).isNotNull();
        assertThat(response.getType()).isEqualTo(ChannelType.EMAIL);
        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(response.getStatus()).isIn(SendStatus.CREATED, SendStatus.SENT);
    }

    @Test
    @Order(11)
    void sendSmsMessage_ShouldCreateMessage() {
        SmsMessageRequest request = new SmsMessageRequest();
        request.setUserId(TEST_USER_ID);
        request.setPhoneNumber("+15551234567");
        request.setMessage("Test SMS message content");

        MessageResponse response = messagesApi.sendSmsMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getUuid()).isNotNull();
        assertThat(response.getType()).isEqualTo(ChannelType.SMS);
        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(response.getStatus()).isIn(SendStatus.CREATED, SendStatus.SENT);
    }

    @Test
    @Order(12)
    void getMessages_ShouldReturnMessages() {
        // Create a message first
        EmailMessageRequest emailRequest = new EmailMessageRequest();
        emailRequest.setUserId(TEST_USER_ID);
        emailRequest.setRecipient("recipient@example.com");
        emailRequest.setSubject("Test Subject");
        emailRequest.setBody("Test Body");
        messagesApi.sendEmailMessage(emailRequest);

        MessageListResponse response = messagesApi.getMessages(1, 10, null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getTotalElements()).isGreaterThan(0);
    }

    @Test
    @Order(13)
    void getMessages_ShouldSupportPagination() {
        // Create multiple messages
        for (int i = 0; i < 5; i++) {
            EmailMessageRequest request = new EmailMessageRequest();
            request.setUserId(TEST_USER_ID);
            request.setRecipient("recipient" + i + "@example.com");
            request.setSubject("Test Subject " + i);
            request.setBody("Test Body " + i);
            messagesApi.sendEmailMessage(request);
        }

        MessageListResponse page1 = messagesApi.getMessages(1, 2, null, null, null);
        assertThat(page1.getItems()).hasSize(2);
        assertThat(page1.getPage()).isEqualTo(1);
        assertThat(page1.getPageSize()).isEqualTo(2);

        MessageListResponse page2 = messagesApi.getMessages(2, 2, null, null, null);
        assertThat(page2.getItems()).hasSize(2);
        assertThat(page2.getPage()).isEqualTo(2);
    }

    // ==================== Admin - Notifications Functionality ====================

    @Test
    @Order(20)
    void sendEmailNotification_ShouldSendNotification() {
        EmailNotificationRequest request = new EmailNotificationRequest();
        request.setRecipient("notification@example.com");
        request.setSubject("Test Notification Subject");
        request.setBody("Test notification body");
        request.setLevel(EmailNotificationRequest.LevelEnum.INFO);

        NotificationResponse response = notificationsApi.sendEmailNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isIn(SendStatus.CREATED, SendStatus.ENQUEUED, SendStatus.SENT);
    }

    @Test
    @Order(21)
    void sendSmsNotification_ShouldSendNotification() {
        SmsNotificationRequest request = new SmsNotificationRequest();
        request.setPhoneNumber("+15551234567");
        request.setMessage("Test SMS notification");
        request.setLevel(SmsNotificationRequest.LevelEnum.WARNING);

        NotificationResponse response = notificationsApi.sendSmsNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isIn(SendStatus.CREATED, SendStatus.ENQUEUED, SendStatus.SENT);
    }

    @Test
    @Order(22)
    void sendSlackNotification_ShouldSendNotification() {
        SlackNotificationRequest request = new SlackNotificationRequest();
        request.setChannel("#test-channel");
        request.setMessage("Test Slack notification message");
        request.setLevel(SlackNotificationRequest.LevelEnum.CRITICAL);

        NotificationResponse response = notificationsApi.sendSlackNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isIn(SendStatus.CREATED, SendStatus.ENQUEUED, SendStatus.SENT);
    }

    // ==================== Validation Tests ====================

    @Test
    @Order(30)
    void sendEmailMessage_WhenMissingRequiredFields_ShouldReturn400() {
        EmailMessageRequest request = new EmailMessageRequest();

        assertThatThrownBy(() -> messagesApi.sendEmailMessage(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(31)
    void sendSmsMessage_WhenInvalidPhoneNumber_ShouldReturn400() {
        SmsMessageRequest request = new SmsMessageRequest();
        request.setUserId(TEST_USER_ID);
        request.setPhoneNumber("invalid-phone");
        request.setMessage("Test SMS");

        assertThatThrownBy(() -> messagesApi.sendSmsMessage(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(32)
    void sendEmailNotification_WhenInvalidEmail_ShouldReturn400() {
        EmailNotificationRequest request = new EmailNotificationRequest();
        request.setRecipient("invalid-email");
        request.setSubject("Test Subject");
        request.setBody("Test Body");

        assertThatThrownBy(() -> notificationsApi.sendEmailNotification(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(33)
    void sendSlackNotification_WhenInvalidChannel_ShouldReturn400() {
        SlackNotificationRequest request = new SlackNotificationRequest();
        request.setChannel("invalid-channel");
        request.setMessage("Test message");

        assertThatThrownBy(() -> notificationsApi.sendSlackNotification(request))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    // ==================== User Can View Own Messages ====================

    @Test
    @Order(40)
    void getMessages_WhenUser_ShouldReturnOnlyOwnMessages() {
        // Admin creates message for user
        EmailMessageRequest request = new EmailMessageRequest();
        request.setUserId(TEST_USER_ID);
        request.setRecipient("user@example.com");
        request.setSubject("Message for user");
        request.setBody("This is a message for the test user");
        messagesApi.sendEmailMessage(request);

        // User should see their own messages
        authenticateAsUser();
        MessageListResponse response = messagesApi.getMessages(1, 10, null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems()).allSatisfy(msg -> 
            assertThat(msg.getUserId()).isEqualTo(TEST_USER_ID)
        );
    }
}

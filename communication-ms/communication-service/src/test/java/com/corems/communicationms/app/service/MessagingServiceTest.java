package com.corems.communicationms.app.service;

import com.corems.communicationms.api.model.MessageListResponse;
import com.corems.communicationms.api.model.MessageResponse;
import com.corems.communicationms.app.entity.EmailMessageEntity;
import com.corems.communicationms.app.entity.SMSMessageEntity;
import com.corems.communicationms.app.model.MessageSenderType;
import com.corems.communicationms.app.model.MessageStatus;
import com.corems.communicationms.app.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MessagingServiceTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessagingService messagingService;

    private UUID testUserId;
    private EmailMessageEntity testEmailMessage;
    private SMSMessageEntity testSmsMessage;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        messageRepository.deleteAll();
        
        testUserId = UUID.randomUUID();

        // Create test email message
        testEmailMessage = new EmailMessageEntity();
        testEmailMessage.setUuid(UUID.randomUUID());
        testEmailMessage.setUserId(testUserId);
        testEmailMessage.setStatus(MessageStatus.sent);
        testEmailMessage.setSentByType(MessageSenderType.system);
        testEmailMessage.setCreatedAt(Instant.now());
        testEmailMessage.setSentAt(Instant.now());
        testEmailMessage.setRecipient("test@example.com");
        testEmailMessage.setSender("system@example.com");
        testEmailMessage.setSubject("Test Subject");
        testEmailMessage.setBody("Test email body");
        testEmailMessage.setEmailType("WELCOME");

        // Create test SMS message
        testSmsMessage = new SMSMessageEntity();
        testSmsMessage.setUuid(UUID.randomUUID());
        testSmsMessage.setUserId(testUserId);
        testSmsMessage.setStatus(MessageStatus.created);
        testSmsMessage.setSentByType(MessageSenderType.user);
        testSmsMessage.setCreatedAt(Instant.now());
        testSmsMessage.setPhoneNumber("+1234567890");
        testSmsMessage.setMessage("Test SMS body");
    }

    @Test
    void listMessages_WhenMessagesExist_ReturnsFilteredList() {
        // Given - save test messages
        messageRepository.save(testEmailMessage);
        messageRepository.save(testSmsMessage);

        // When
        MessageListResponse response = messagingService.listMessages(
            testUserId,
            Optional.of(1),
            Optional.of(10),
            Optional.of("createdAt:desc"),
            Optional.empty(),
            Optional.empty()
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        
        List<UUID> messageIds = response.getItems().stream()
            .map(MessageResponse::getUuid)
            .toList();
        assertThat(messageIds).containsExactlyInAnyOrder(
            testEmailMessage.getUuid(), 
            testSmsMessage.getUuid()
        );
    }

    @Test
    void listMessages_WhenFilterByMessageType_ReturnsFilteredResults() {
        // Given - save test messages
        messageRepository.save(testEmailMessage);
        messageRepository.save(testSmsMessage);

        // When - filter by EMAIL message type
        MessageListResponse response = messagingService.listMessages(
            testUserId,
            Optional.of(1),
            Optional.of(10),
            Optional.empty(),
            Optional.empty(),
            Optional.of(List.of("type:eq:email"))
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getUuid()).isEqualTo(testEmailMessage.getUuid());
    }

    @Test
    void listMessages_WhenSearchByUserId_ReturnsMatchingResults() {
        // Given - save test messages with different user IDs
        messageRepository.save(testEmailMessage);
        messageRepository.save(testSmsMessage);
        
        // Create a message with different user ID
        UUID differentUserId = UUID.randomUUID();
        EmailMessageEntity differentUserMessage = new EmailMessageEntity();
        differentUserMessage.setUuid(UUID.randomUUID());
        differentUserMessage.setUserId(differentUserId);
        differentUserMessage.setStatus(MessageStatus.sent);
        differentUserMessage.setSentByType(MessageSenderType.system);
        differentUserMessage.setCreatedAt(Instant.now());
        differentUserMessage.setRecipient("other@example.com");
        differentUserMessage.setSender("system@example.com");
        differentUserMessage.setSubject("Other Subject");
        differentUserMessage.setBody("Other email body");
        differentUserMessage.setEmailType("OTHER");
        messageRepository.save(differentUserMessage);

        // When - search by user ID (convert to string for search)
        MessageListResponse response = messagingService.listMessages(
            testUserId,
            Optional.of(1),
            Optional.of(10),
            Optional.empty(),
            Optional.of(testUserId.toString()),
            Optional.empty()
        );

        // Then - should find messages for testUserId only
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().stream().map(MessageResponse::getUuid))
            .containsExactlyInAnyOrder(testEmailMessage.getUuid(), testSmsMessage.getUuid());
    }

    @Test
    void listMessages_WhenDifferentUser_ReturnsEmptyList() {
        // Given - save test messages for testUserId
        messageRepository.save(testEmailMessage);
        messageRepository.save(testSmsMessage);

        UUID differentUserId = UUID.randomUUID();

        // When - query with different user ID
        MessageListResponse response = messagingService.listMessages(
            differentUserId,
            Optional.of(1),
            Optional.of(10),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );

        // Then - should return empty list (user isolation)
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
    }

    @Test
    void mapEmail_WhenEmailEntity_ReturnsEmailPayload() {
        // Given - save email message
        messageRepository.save(testEmailMessage);

        // When
        var emailPayload = messagingService.mapEmail(testEmailMessage);

        // Then
        assertThat(emailPayload).isNotNull();
        assertThat(emailPayload.getSubject()).isEqualTo("Test Subject");
        assertThat(emailPayload.getRecipient()).isEqualTo("test@example.com");
        assertThat(emailPayload.getBody()).isEqualTo("Test email body");
    }

    @Test
    void mapSms_WhenSmsEntity_ReturnsSmsPayload() {
        // Given - save SMS message
        messageRepository.save(testSmsMessage);

        // When
        var smsPayload = messagingService.mapSms(testSmsMessage);

        // Then
        assertThat(smsPayload).isNotNull();
        assertThat(smsPayload.getPhoneNumber()).isEqualTo("+1234567890");
        assertThat(smsPayload.getMessage()).isEqualTo("Test SMS body");
    }

    @Test
    void listMessages_WhenFilterByStatus_ReturnsFilteredResults() {
        // Given - save test messages with different statuses
        messageRepository.save(testEmailMessage); // status: sent
        messageRepository.save(testSmsMessage);   // status: created

        // When - filter by sent status
        MessageListResponse response = messagingService.listMessages(
            testUserId,
            Optional.of(1),
            Optional.of(10),
            Optional.empty(),
            Optional.empty(),
            Optional.of(List.of("status:eq:sent"))
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getUuid()).isEqualTo(testEmailMessage.getUuid());
    }

    @Test
    void listMessages_WhenNoMessages_ReturnsEmptyList() {
        // When - query with no messages in database
        MessageListResponse response = messagingService.listMessages(
            testUserId,
            Optional.of(1),
            Optional.of(10),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
    }
}
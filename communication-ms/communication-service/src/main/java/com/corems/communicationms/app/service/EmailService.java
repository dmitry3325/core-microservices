package com.corems.communicationms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.communicationms.api.model.ChannelType;
import com.corems.communicationms.api.model.EmailMessageRequest;
import com.corems.communicationms.api.model.EmailNotificationRequest;
import com.corems.communicationms.api.model.EmailPayload;
import com.corems.communicationms.api.model.MessageResponse;
import com.corems.communicationms.api.model.NotificationResponse;
import com.corems.communicationms.api.model.SendStatus;
import com.corems.communicationms.app.config.MailConfig;
import com.corems.communicationms.app.entity.EmailMessageEntity;
import com.corems.communicationms.app.model.MessageStatus;
import com.corems.communicationms.app.model.MessageType;
import com.corems.communicationms.app.repository.MessageRepository;
import com.corems.communicationms.app.service.provider.EmailServiceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailService {
    private final MailConfig config;
    private final MessageRepository messageRepository;
    private final EmailServiceProvider emailServiceProvider;
    private final MessageDispatcher messageDispatcher;

    public MessageResponse sendMessage(EmailMessageRequest emailRequest) {
        EmailMessageEntity emailEntity = createEntity(emailRequest);
        EmailPayload payload = getPayload(emailRequest);
        try {
            MessageStatus status = messageDispatcher.dispatchMessage(emailServiceProvider, MessageType.EMAIL, payload);
            emailEntity.setStatus(status);
        } catch (ServiceException exception) {
            log.error("Failed to send email message: ", exception);

            emailEntity.setStatus(MessageStatus.FAILED);
            emailEntity.setUpdatedAt(Instant.now());
            messageRepository.save(emailEntity);
            throw exception;
        }

        MessageResponse response = new MessageResponse();
        response.setUuid(emailEntity.getUuid());
        response.setUserId(emailEntity.getUserId());
        response.setType(ChannelType.EMAIL);
        response.setStatus(SendStatus.valueOf(emailEntity.getStatus().toString()));
        response.setCreatedAt(emailEntity.getCreatedAt().atOffset(ZoneOffset.UTC));
        response.setPayload(payload);

        return response;
    }

    public NotificationResponse sendNotification(EmailNotificationRequest emailRequest) {
        try {
            EmailPayload payload = getPayload(emailRequest);
            MessageStatus status = messageDispatcher.dispatchMessage(emailServiceProvider, MessageType.EMAIL, payload);

            NotificationResponse response = new NotificationResponse();
            response.setStatus(SendStatus.valueOf(status.toString()));
            response.setSentAt(Instant.now().atOffset(ZoneOffset.UTC));

            return response;
        } catch (ServiceException exception) {
            log.error("Failed to send email message: ", exception);
            throw exception;
        }
    }

    private EmailPayload getPayload(EmailMessageRequest emailRequest) {
        EmailPayload payload = new EmailPayload(emailRequest.getSubject(), emailRequest.getRecipient(), emailRequest.getBody());
        payload.setSender(emailRequest.getSender() == null ? config.getDefaultFrom() : emailRequest.getSender());
        payload.setEmailType(EmailPayload.EmailTypeEnum.valueOf(emailRequest.getEmailType().getValue()));
        payload.setSenderName(emailRequest.getSenderName());
        payload.setCc(emailRequest.getCc());
        payload.setBcc(emailRequest.getBcc());
        return payload;
    }

    private EmailPayload getPayload(EmailNotificationRequest emailRequest) {
        EmailPayload payload = new EmailPayload(emailRequest.getSubject(), emailRequest.getRecipient(), emailRequest.getBody());
        payload.setSender(emailRequest.getSender() == null ? config.getDefaultFrom() : emailRequest.getSender());
        payload.setEmailType(EmailPayload.EmailTypeEnum.valueOf(emailRequest.getEmailType().getValue()));
        payload.setSenderName(emailRequest.getSenderName());
        payload.setCc(emailRequest.getCc());
        payload.setBcc(emailRequest.getBcc());
        return payload;
    }

    private EmailMessageEntity createEntity(EmailMessageRequest emailRequest) {
        EmailMessageEntity emailEntity = new EmailMessageEntity();
        emailEntity.setUuid(UUID.randomUUID());
        emailEntity.setEmailType(emailRequest.getEmailType() == null ? "TXT" : emailRequest.getEmailType().toString());
        emailEntity.setSubject(emailRequest.getSubject());
        emailEntity.setSender(emailRequest.getSender() == null ? config.getDefaultFrom() : emailRequest.getSender());
        emailEntity.setSenderName(emailRequest.getSenderName());
        if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
            emailEntity.setCc(String.join(",", emailRequest.getCc()));
        }
        if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
            emailEntity.setBcc(String.join(",", emailRequest.getBcc()));
        }
        emailEntity.setRecipient(emailRequest.getRecipient());
        emailEntity.setBody(emailRequest.getBody());
        emailEntity.setUserId(emailRequest.getUserId());
        emailEntity.setCreatedAt(Instant.now());
        emailEntity.setStatus(MessageStatus.CREATED);

        messageRepository.save(emailEntity);
        return emailEntity;
    }
}

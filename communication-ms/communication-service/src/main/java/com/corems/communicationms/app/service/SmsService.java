package com.corems.communicationms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.security.SecurityUtils;
import com.corems.communicationms.api.model.ChannelType;
import com.corems.communicationms.api.model.SmsMessageRequest;
import com.corems.communicationms.api.model.SmsNotificationRequest;
import com.corems.communicationms.api.model.SmsPayload;
import com.corems.communicationms.api.model.MessageResponse;
import com.corems.communicationms.api.model.NotificationResponse;
import com.corems.communicationms.api.model.SendStatus;
import com.corems.communicationms.app.entity.SMSMessageEntity;
import com.corems.communicationms.app.model.MessageStatus;
import com.corems.communicationms.app.model.MessageSenderType;
import com.corems.communicationms.app.repository.MessageRepository;
import com.corems.communicationms.app.service.provider.SmsServiceProvider;
import com.corems.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.corems.communicationms.api.model.MessageResponse.SentByTypeEnum;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsService {
    private final MessageRepository messageRepository;
    private final SmsServiceProvider smsServiceProvider;
    private final MessageDispatcher messageDispatcher;

    public MessageResponse sendMessage(SmsMessageRequest smsRequest) {
        SMSMessageEntity smsEntity = createEntity(smsRequest);
        SmsPayload payload = getPayload(smsRequest);
        try {
            MessageStatus status = messageDispatcher.dispatchMessage(smsServiceProvider, smsEntity.getUuid(), payload);
            smsEntity.setStatus(status);
            messageRepository.save(smsEntity);
        } catch (ServiceException exception) {
            log.error("Failed to send SMS message: ", exception);

            smsEntity.setStatus(MessageStatus.failed);
            smsEntity.setSentAt(Instant.now());
            messageRepository.save(smsEntity);
            throw exception;
        }

        MessageResponse response = new MessageResponse();
        response.setUuid(smsEntity.getUuid());
        response.setUserId(smsEntity.getUserId());
        response.setType(ChannelType.SMS);
        response.setStatus(SendStatus.fromValue(smsEntity.getStatus().toString()));
        response.setCreatedAt(smsEntity.getCreatedAt().atOffset(ZoneOffset.UTC));
        response.setPayload(payload);
        response.setSentById(smsEntity.getSentById());
        if (smsEntity.getSentByType() != null) {
            response.setSentByType(SentByTypeEnum.fromValue(smsEntity.getSentByType().name()));
        }

        return response;
    }

    public NotificationResponse sendNotification(SmsNotificationRequest smsRequest) {
        try {
            SmsPayload payload = getPayload(smsRequest);
            MessageStatus status = messageDispatcher.dispatchMessage(smsServiceProvider, UUID.randomUUID(), payload);

            NotificationResponse response = new NotificationResponse();
            response.setStatus(SendStatus.fromValue(status.toString()));
            response.setSentAt(Instant.now().atOffset(ZoneOffset.UTC));

            return response;
        } catch (ServiceException exception) {
            log.error("Failed to send SMS notification: ", exception);
            throw exception;
        }
    }

    private SmsPayload getPayload(SmsMessageRequest smsRequest) {
        return new SmsPayload(ChannelType.SMS.getValue(), smsRequest.getPhoneNumber(), smsRequest.getMessage());
    }

    private SmsPayload getPayload(SmsNotificationRequest smsRequest) {
        return new SmsPayload(ChannelType.SMS.getValue(), smsRequest.getPhoneNumber(), smsRequest.getMessage());
    }

    private SMSMessageEntity createEntity(SmsMessageRequest smsRequest) {
        SMSMessageEntity smsEntity = new SMSMessageEntity();
        smsEntity.setUuid(UUID.randomUUID());
        smsEntity.setPhoneNumber(smsRequest.getPhoneNumber());
        smsEntity.setMessage(smsRequest.getMessage());
        smsEntity.setUserId(smsRequest.getUserId());
        smsEntity.setCreatedAt(Instant.now());
        smsEntity.setStatus(MessageStatus.created);

        UserPrincipal userPrincipal = SecurityUtils.getUserPrincipal();
        if (userPrincipal.getUserId() != null) {
            smsEntity.setSentById(userPrincipal.getUserId());
            smsEntity.setSentByType(MessageSenderType.user);
        } else {
            smsEntity.setSentByType(MessageSenderType.system);
        }

        messageRepository.save(smsEntity);
        return smsEntity;
    }
}

package com.corems.communicationms.app.service.provider;

import com.corems.common.security.UserPrincipal;
import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.communicationms.app.config.SmsConfig;
import com.corems.communicationms.app.entity.MessageEntity;
import com.corems.communicationms.app.entity.SMSMessageEntity;
import com.corems.communicationms.api.model.SmsRequest;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Service
public class SmsServiceProvider extends MessagingServiceProvider<SMSMessageEntity, SmsRequest> {

    private final SmsConfig config;

    @Autowired
    public SmsServiceProvider(SmsConfig config) {
        this.config = config;
        if (config.enabled()) {
            Twilio.init(config.accountSid(), config.authToken());
        }
    }

    @Override
    public SMSMessageEntity sendDirect(SMSMessageEntity entity) {
        try {
            if (!config.enabled()) {
                log.info("SMS sending disabled@! Simulating send to {}: {}", entity.getPhoneNumber(), entity.getMessage());
            } else {
                Message message = Message.creator(
                        new PhoneNumber(entity.getPhoneNumber()),
                        new PhoneNumber(config.fromNumber()),
                        entity.getMessage()
                ).create();

                log.info("SMS sent via Twilio, SID: {}", message.getSid());
                entity.setSid(message.getSid());
            }

            entity.setStatus(MessageEntity.MessageStatus.SENT);
            entity.setUpdatedAt(Instant.now());
            messageRepository.save(entity);

            return entity;
        } catch (Exception ex) {
            entity.setStatus(MessageEntity.MessageStatus.FAILED);
            entity.setUpdatedAt(Instant.now());
            messageRepository.save(entity);

            log.error("Failed to send SMS message: ", ex);
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Unable to send SMS message.");
        }
    }

    @Override
    protected SMSMessageEntity createEntityAndSave(SmsRequest smsRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        SMSMessageEntity sms = new SMSMessageEntity();
        sms.setUuid(UUID.randomUUID().toString());
        sms.setPhoneNumber(smsRequest.getPhoneNumber());
        sms.setMessage(smsRequest.getMessage());

        sms.setUserId(userPrincipal.getUserId());
        sms.setCreatedAt(Instant.now());
        sms.setStatus(MessageEntity.MessageStatus.CREATED);
        messageRepository.save(sms);

        return sms;
    }
}

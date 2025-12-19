package com.corems.communicationms.app.service.provider;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.communicationms.api.model.SmsPayload;
import com.corems.communicationms.app.config.SmsConfig;
import com.corems.communicationms.app.model.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsServiceProvider implements ChannelProvider<SmsPayload> {
    private final SmsConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SmsServiceProvider(SmsConfig config) {
        this.config = config;
        if (Boolean.TRUE.equals(config.enabled())) {
            Twilio.init(config.accountSid(), config.authToken());
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.sms;
    }

    @Override
    public void convertAndSend(Object payload) {
        send(objectMapper.convertValue(payload, SmsPayload.class));
    }

    @Override
    public void send(SmsPayload payload) {
        if (Boolean.FALSE.equals(config.enabled())) {
            log.info("SMS sending disabled@! Simulating send to {}: {}", payload.getPhoneNumber(), payload.getMessage());
            return;
        }
        try {
            Message message = Message.creator(
                    new PhoneNumber(payload.getPhoneNumber()),
                    new PhoneNumber(config.fromNumber()),
                    payload.getMessage()
            ).create();

            // TODO find out what we do about it
            log.info("SMS sent via Twilio, SID: {}", message.getSid());
        } catch (Exception ex) {
            log.error("Failed to send SMS message: ", ex);
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Unable to send SMS message.");
        }
    }
}

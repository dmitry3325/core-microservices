package com.corems.communicationms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.communicationms.api.model.SlackNotificationRequest;
import com.corems.communicationms.api.model.SlackPayload;
import com.corems.communicationms.api.model.MessageResponse;
import com.corems.communicationms.api.model.NotificationResponse;
import com.corems.communicationms.api.model.SendStatus;
import com.corems.communicationms.app.model.MessageStatus;
import com.corems.communicationms.app.service.provider.SlackServiceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackService {
    private final SlackServiceProvider slackServiceProvider;
    private final MessageDispatcher messageDispatcher;

    public MessageResponse sendMessage(SlackNotificationRequest slackRequest) {
        throw ServiceException.of(
                DefaultExceptionReasonCodes.NOT_IMPLEMENTED,
                "Slack messages are not supported. Use notifications instead."
        );
    }

    public NotificationResponse sendNotification(SlackNotificationRequest slackRequest) {
        try {
            SlackPayload payload = getPayload(slackRequest);
            MessageStatus status = messageDispatcher.dispatchMessage(slackServiceProvider, UUID.randomUUID(), payload);

            NotificationResponse response = new NotificationResponse();
            response.setStatus(SendStatus.fromValue(status.toString()));
            response.setSentAt(Instant.now().atOffset(ZoneOffset.UTC));

            return response;
        } catch (ServiceException exception) {
            log.error("Failed to send Slack notification: ", exception);
            throw exception;
        }
    }

    private SlackPayload getPayload(SlackNotificationRequest slackRequest) {
        return new SlackPayload(slackRequest.getChannel(), slackRequest.getMessage());
    }
}


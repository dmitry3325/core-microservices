package com.corems.notificationms.service.provider;

import com.corems.notificationms.config.SlackConfig;
import com.corems.notificationms.entity.NotificationEntity;
import com.corems.notificationms.entity.SlackNotificationEntity;
import com.corems.notificationms.exception.NotificationServiceException;
import com.corems.notificationms.model.Notification;
import com.corems.notificationms.model.SlackNotification;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SlackServiceProvider implements NotificationServiceProvider {

    private static final Logger logger = LoggerFactory.getLogger(SlackServiceProvider.class);

    private final SlackConfig config;

    private final MethodsClient client;

    public SlackServiceProvider(SlackConfig slackConfig) {
        this.config = slackConfig;
        this.client = Slack.getInstance().methods(config.token());
    }

    @Override
    public NotificationEntity sendMessage(Notification notification) {
        if (config.enabled()) {
            throw new NotificationServiceException("Method Slack is not available. To enable provider contact support service.");
        }

        final SlackNotification slackRequest = (SlackNotification) notification;

        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .channel(slackRequest.getChannel())
                .text(slackRequest.getMessage())
                .build();

        try {
            ChatPostMessageResponse response = client.chatPostMessage(request);
            if (!response.isOk()) {
                throw new RuntimeException("Slack error: " + response.getError());
            }
        } catch (SlackApiException | IOException ex) {
            logger.error("Unable to send slack message: ", ex);
            throw new NotificationServiceException("Unable to send slack message.");
        }

        final SlackNotificationEntity notificationEntity = new SlackNotificationEntity();
        notificationEntity.setChannel(slackRequest.getChannel());
        notificationEntity.setMessage(slackRequest.getMessage());

        return notificationEntity;
    }

}
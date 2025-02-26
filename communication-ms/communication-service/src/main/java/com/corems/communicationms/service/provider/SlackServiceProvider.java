package com.corems.communicationms.service.provider;

import com.corems.communicationms.config.SlackConfig;
import com.corems.communicationms.entity.MessageEntity;
import com.corems.communicationms.entity.SlackMessageEntity;
import com.corems.communicationms.exception.NotificationServiceException;
import com.corems.communicationms.model.MessageRequest;
import com.corems.communicationms.model.MessageRequest;
import com.corems.communicationms.model.SlackRequest;
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
public class SlackServiceProvider implements MessagingServiceProvider {

    private static final Logger logger = LoggerFactory.getLogger(SlackServiceProvider.class);

    private final SlackConfig config;

    private final MethodsClient client;

    public SlackServiceProvider(SlackConfig slackConfig) {
        this.config = slackConfig;
        this.client = Slack.getInstance().methods(config.token());
    }

    @Override
    public MessageEntity sendMessage(MessageRequest messageRequest) {
        if (config.enabled()) {
            throw new NotificationServiceException("Method Slack is not available. To enable provider contact support service.");
        }

        final SlackRequest slackRequest = (SlackRequest) messageRequest;

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

        final SlackMessageEntity notificationEntity = new SlackMessageEntity();
        notificationEntity.setChannel(slackRequest.getChannel());
        notificationEntity.setMessage(slackRequest.getMessage());

        return notificationEntity;
    }

}
package com.corems.communicationms.app.service.provider;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.communicationms.api.model.SlackPayload;
import com.corems.communicationms.app.config.SlackConfig;
import com.corems.communicationms.app.model.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class SlackServiceProvider implements ChannelProvider<SlackPayload> {
    private final SlackConfig config;
    private final MethodsClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SlackServiceProvider(SlackConfig slackConfig) {
        this.config = slackConfig;
        this.client = Slack.getInstance().methods(config.token());
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.slack;
    }

    @Override
    public void convertAndSend(Object payload) {
        send(objectMapper.convertValue(payload, SlackPayload.class));
    }

    @Override
    public void send(SlackPayload payload) {
        if (!config.enabled()) {
            log.info("Slack sending disabled! Simulating send to channel {}: {}", payload.getChannel(), payload.getMessage());
            return;
        }

        try {
            ChatPostMessageResponse response = client.chatPostMessage(
                    ChatPostMessageRequest.builder()
                            .channel(payload.getChannel())
                            .text(payload.getMessage())
                            .build());
            if (!response.isOk()) {
                throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Slack error: " + response.getError());
            }
        } catch (SlackApiException | IOException ex) {
            log.error("Unable to send slack message: ", ex);
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Unable to send slack message.");
        }
    }
}
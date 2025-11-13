package com.corems.communicationms.service.provider;

import com.corems.common.security.UserPrincipal;
import com.corems.common.service.exception.ServiceException;
import com.corems.common.service.exception.handler.DefaultExceptionReasonCodes;
import com.corems.communicationms.config.SlackConfig;
import com.corems.communicationms.entity.MessageEntity;
import com.corems.communicationms.entity.SlackMessageEntity;
import com.corems.communicationms.model.SlackRequest;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class SlackServiceProvider extends MessagingServiceProvider<SlackMessageEntity, SlackRequest> {
    private final SlackConfig config;

    private final MethodsClient client;

    @Autowired
    public SlackServiceProvider(SlackConfig slackConfig) {
        this.config = slackConfig;
        this.client = Slack.getInstance().methods(config.token());
    }

    @Override
    public SlackMessageEntity sendDirect(SlackMessageEntity slackMessageEntity) {
        try {
            if (!config.enabled()) {
                log.info("Slack sending disabled! Simulating send to channel {}: {}", slackMessageEntity.getChannel(), slackMessageEntity.getMessage());
            } else {
                ChatPostMessageResponse response = client.chatPostMessage(
                        ChatPostMessageRequest.builder()
                                .channel(slackMessageEntity.getChannel())
                                .text(slackMessageEntity.getMessage())
                                .build());
                if (!response.isOk()) {
                    throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Slack error: " + response.getError());
                }
            }
        } catch (SlackApiException | IOException ex) {
            log.error("Unable to send slack message: ", ex);
            messageRepository.findById(slackMessageEntity.getId()).ifPresent(e -> {
                e.setStatus(MessageEntity.MessageStatus.FAILED);
                e.setUpdatedAt(Instant.now());
                messageRepository.save(e);
            });

            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Unable to send slack message.");
        }

        slackMessageEntity.setStatus(MessageEntity.MessageStatus.SENT);
        slackMessageEntity.setUpdatedAt(Instant.now());
        messageRepository.save(slackMessageEntity);

        return slackMessageEntity;
    }

    @Override
    protected SlackMessageEntity createEntityAndSave(SlackRequest slackRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        SlackMessageEntity slackMessageEntity = new SlackMessageEntity();
        slackMessageEntity.setUuid(UUID.randomUUID().toString());
        slackMessageEntity.setChannel(slackRequest.getChannel());
        slackMessageEntity.setMessage(slackRequest.getMessage());
        slackMessageEntity.setUserId(userPrincipal.getUserId());
        slackMessageEntity.setCreatedAt(Instant.now());
        slackMessageEntity.setStatus(MessageEntity.MessageStatus.CREATED);

        messageRepository.save(slackMessageEntity);

        return slackMessageEntity;
    }

}
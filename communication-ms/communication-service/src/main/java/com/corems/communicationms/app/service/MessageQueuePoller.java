package com.corems.communicationms.app.service;

import com.corems.common.queue.QueueProvider;
import com.corems.common.queue.poller.GenericQueuePoller;
import com.corems.common.queue.poller.MessageHandler;
import com.corems.communicationms.app.entity.MessageEntity;
import com.corems.communicationms.app.model.MessageStatus;
import com.corems.communicationms.app.repository.MessageRepository;
import com.corems.communicationms.app.service.provider.ChannelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true")
public class MessageQueuePoller {
    private final MessageRepository messageRepository;
    private final GenericQueuePoller genericPoller;

    public MessageQueuePoller(QueueProvider queueProvider,
                              MessageRepository messageRepository,
                              Map<String, ChannelProvider<?>> channelProviders) {

        this.messageRepository = messageRepository;
        this.genericPoller = new GenericQueuePoller(queueProvider.getDefaultClient(), getQueueHandlers(channelProviders));
    }

    private Map<String, MessageHandler> getQueueHandlers(Map<String, ChannelProvider<?>> channelProviders) {
        return channelProviders.values().stream()
                .collect(Collectors.toMap(
                        // message type
                        cp -> cp.getMessageType().toString(),
                        // message handler
                        cp -> qm -> {
                            UUID uuid = UUID.fromString(qm.getId());
                            Optional<MessageEntity> messageOpt = messageRepository.findByUuid(uuid);
                            log.info("messageOpt {}, ID: {}", messageOpt.isPresent(), qm.getId());
                            try {
                                cp.convertAndSend(qm.getPayload());
                                if (messageOpt.isPresent()) {
                                    MessageEntity message = messageOpt.get();
                                    message.setSentAt(Instant.now());
                                    message.setStatus(MessageStatus.sent);
                                    messageRepository.save(message);
                                }
                            } catch (Exception ex) {
                                if (messageOpt.isPresent()) {
                                    MessageEntity message = messageOpt.get();
                                    message.setSentAt(Instant.now());
                                    message.setStatus(MessageStatus.failed);
                                    messageRepository.save(message);
                                }
                                throw ex;
                            }
                        }
                ));
    }

    @PostConstruct
    public void start() {
        log.info("MessageQueuePoller initialized and delegated to GenericQueuePoller");
    }

    @PreDestroy
    public void stop() {
        try {
            genericPoller.close();
        } catch (Exception e) {
            log.warn("Failed to stop GenericQueuePoller: {}", e.getMessage());
        }
    }
}

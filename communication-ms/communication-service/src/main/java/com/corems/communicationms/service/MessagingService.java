package com.corems.communicationms.service;

import com.corems.communicationms.entity.EmailMessageEntity;
import com.corems.communicationms.entity.MessageEntity;
import com.corems.communicationms.entity.SMSMessageEntity;
import com.corems.communicationms.model.MessageRequest;
import com.corems.communicationms.model.MessageResponse;
import com.corems.communicationms.repository.MessageRepository;
import com.corems.communicationms.service.provider.SlackServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class MessagingService {

    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);

    private final MessageRepository messageRepository;

    private final SlackServiceProvider slackServiceProvider;

    public MessagingService(
            MessageRepository notificationRepository,
            SlackServiceProvider slackServiceProvider
    ) {
        this.messageRepository = notificationRepository;
        this.slackServiceProvider = slackServiceProvider;
    }

    public MessageResponse sendMessage(MessageRequest message) {
        MessageEntity notificationEntity = switch (message.getType()) {
            case SMS -> new SMSMessageEntity();
            case SLACK -> slackServiceProvider.sendMessage(message);
            case EMAIL -> new EmailMessageEntity();
        };

        messageRepository.save(notificationEntity);

        MessageResponse response = new MessageResponse();
        response.setId(notificationEntity.getId().toString());
        response.setType(notificationEntity.getType().getValue());
        response.createdAt(notificationEntity.getCreatedAt());
        response.setData(message);

        return response;
    }
}
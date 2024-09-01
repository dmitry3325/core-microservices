package com.corems.notificationms.service;

import com.corems.notificationms.entity.EmailNotificationEntity;
import com.corems.notificationms.entity.NotificationEntity;
import com.corems.notificationms.entity.SMSNotificationEntity;
import com.corems.notificationms.model.NotificationResponse;
import com.corems.notificationms.model.Notification;
import com.corems.notificationms.repository.NotificationRepository;
import com.corems.notificationms.service.provider.SlackServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    private final SlackServiceProvider slackServiceProvider;

    public NotificationService(
            NotificationRepository notificationRepository,
            SlackServiceProvider slackServiceProvider
    ) {
        this.notificationRepository = notificationRepository;
        this.slackServiceProvider = slackServiceProvider;
    }

    public NotificationResponse sendNotification(Notification notification) {
        NotificationEntity notificationEntity = switch (notification.getType()) {
            case SMS -> new SMSNotificationEntity();
            case SLACK -> slackServiceProvider.sendMessage(notification);
            case EMAIL -> new EmailNotificationEntity();
        };

        notificationRepository.save(notificationEntity);

        NotificationResponse response = new NotificationResponse();
        response.setId(notificationEntity.getId().toString());
        response.setType(notificationEntity.getType().getValue());
        response.createdAt(notificationEntity.getCreatedAt());
        response.setData(notification);

        return response;
    }
}
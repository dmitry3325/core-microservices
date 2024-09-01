package com.corems.notificationms.service.provider;

import com.corems.notificationms.entity.NotificationEntity;
import com.corems.notificationms.model.Notification;

public interface NotificationServiceProvider {
    NotificationEntity sendMessage(Notification notificationRequest);
}

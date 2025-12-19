package com.corems.userms.app.service;

import com.corems.communicationms.api.model.EmailNotificationRequest;
import com.corems.communicationms.api.model.SmsNotificationRequest;
import com.corems.communicationms.client.NotificationsApi;
import com.corems.userms.app.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationsApi notificationsApi;

    @Async
    public void sendWelcomeEmail(UserEntity user) {
        try {
            EmailNotificationRequest request = new EmailNotificationRequest();
            request.setSubject("Welcome to CoreMS");
            request.setRecipient(user.getEmail());
            request.setBody("Dear " + user.getFirstName() + ",\n\n" +
                    "Welcome to CoreMS! We're excited to have you on board.\n\n" +
                    "Best regards,\n" +
                    "The CoreMS Team");

            var res = notificationsApi.sendEmailNotification(request);

            log.info("Welcome email sent to user: {}, result: {}", user.getUuid(), res);
        } catch (Exception e) {
            log.error("Failed to send welcome email to user: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendWelcomeSms(UserEntity user) {
        if (user.getPhoneNumber() == null) {
            log.debug("No phone number for user: {}, skipping SMS", user.getUuid());
            return;
        }

        try {
            SmsNotificationRequest request = new SmsNotificationRequest();
            request.setPhoneNumber(user.getPhoneNumber());
            request.setMessage("Welcome to CoreMS, " + user.getFirstName() + "!");

            var res = notificationsApi.sendSmsNotification(request);

            log.info("Welcome SMS sent to user: {}, result: {}", user.getUuid(), res);
        } catch (Exception e) {
            log.error("Failed to send welcome SMS to user: {}", user.getPhoneNumber(), e);
        }
    }
}

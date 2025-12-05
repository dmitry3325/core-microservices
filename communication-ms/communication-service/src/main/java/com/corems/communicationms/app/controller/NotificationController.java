package com.corems.communicationms.app.controller;

import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.RequireRoles;
import com.corems.communicationms.api.NotificationsApi;
import com.corems.communicationms.api.model.EmailNotificationRequest;
import com.corems.communicationms.api.model.NotificationResponse;
import com.corems.communicationms.api.model.SlackNotificationRequest;
import com.corems.communicationms.api.model.SmsNotificationRequest;
import com.corems.communicationms.app.service.EmailService;
import com.corems.communicationms.app.service.SlackService;
import com.corems.communicationms.app.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController implements NotificationsApi {

    private final EmailService emailService;
    private final SmsService smsService;
    private final SlackService slackService;

    @Override
    @RequireRoles(CoreMsRoles.COMMUNICATION_MS_ADMIN)
    public ResponseEntity<NotificationResponse> sendEmailNotification(EmailNotificationRequest emailNotificationRequest) {
        log.info("Received Email notification: {}", emailNotificationRequest);
        NotificationResponse response = emailService.sendNotification(emailNotificationRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Override
    @RequireRoles(CoreMsRoles.COMMUNICATION_MS_ADMIN)
    public ResponseEntity<NotificationResponse> sendSlackNotification(SlackNotificationRequest slackNotificationRequest) {
        log.info("Received Slack notification: {}", slackNotificationRequest);
        NotificationResponse response = slackService.sendNotification(slackNotificationRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Override
    @RequireRoles(CoreMsRoles.COMMUNICATION_MS_ADMIN)
    public ResponseEntity<NotificationResponse> sendSmsNotification(SmsNotificationRequest smsNotificationRequest) {
        log.info("Received SMS notification: {}", smsNotificationRequest);
        NotificationResponse response = smsService.sendNotification(smsNotificationRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
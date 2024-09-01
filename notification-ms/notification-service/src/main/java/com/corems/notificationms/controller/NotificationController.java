package com.corems.notificationms.controller;

import com.corems.notificationms.api.NotificationApi;
import com.corems.notificationms.model.Notification;
import com.corems.notificationms.model.NotificationResponse;
import com.corems.notificationms.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class NotificationController implements NotificationApi {

    public NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public ResponseEntity<List<NotificationResponse>> getNotificationStatusList(
            @RequestParam(value = "type", required = false) Optional<String> type,
            @RequestParam(value = "userId", required = false) Optional<String> userId,
            @RequestParam(value = "fromDate", required = false) Optional<OffsetDateTime> fromDate,
            @RequestParam(value = "toDate", required = false) Optional<OffsetDateTime> toDate) {

        return ResponseEntity.ok(List.of());
    }

    @Override
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody Notification notification) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.notificationService.sendNotification(notification));
    }
}

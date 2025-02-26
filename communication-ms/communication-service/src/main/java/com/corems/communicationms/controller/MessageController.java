package com.corems.communicationms.controller;


import com.corems.communicationms.api.MessagesApi;
import com.corems.communicationms.model.MessageRequest;
import com.corems.communicationms.model.MessageResponse;
import com.corems.communicationms.service.MessagingService;
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
public class MessageController implements MessagesApi {

    public MessagingService messagingService;

    @Autowired
    public MessageController(MessagingService notificationService) {
        this.messagingService = notificationService;
    }

    @Override
    public ResponseEntity<List<MessageResponse>> getNotificationStatusList(
            @RequestParam(value = "type", required = false) Optional<String> type,
            @RequestParam(value = "userId", required = false) Optional<String> userId,
            @RequestParam(value = "fromDate", required = false) Optional<OffsetDateTime> fromDate,
            @RequestParam(value = "toDate", required = false) Optional<OffsetDateTime> toDate) {

        return ResponseEntity.ok(List.of());
    }

    @Override
    public ResponseEntity<MessageResponse> sendNotification(@Valid @RequestBody MessageRequest messageRequest) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.messagingService.sendMessage(messageRequest));
    }
}

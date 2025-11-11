package com.corems.communicationms.controller;


import com.corems.communicationms.api.MessageApi;
import com.corems.communicationms.model.GetMessagesForUser;
import com.corems.communicationms.model.MessageRequest;
import com.corems.communicationms.model.MessageResponse;
import com.corems.communicationms.service.MessagingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
public class MessageController implements MessageApi {

    public MessagingService messagingService;

    @Autowired
    public MessageController(MessagingService notificationService) {
        this.messagingService = notificationService;
    }

    @Override
    public ResponseEntity<GetMessagesForUser> getMessagesForUser(
            String userId,
            Optional<String> type,
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> sort,
            Optional<String> search,
            Optional<List<String>> filter) {

        log.debug("Authenticated user: {}", SecurityContextHolder.getContext().getAuthentication());

        GetMessagesForUser resp = this.messagingService.listMessages(userId, type, page, pageSize, sort, search, filter);

        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<MessageResponse> sendMessageForUser(String userId, @Valid MessageRequest messageRequest) {
        MessageResponse result = this.messagingService.sendMessage(messageRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    @Override
    public ResponseEntity<MessageResponse> getMessageById(String userId, String messageId) {
        // TODO: implement retrieval by id; for now return 200 with empty body if not found
        MessageResponse resp = new MessageResponse();
        return ResponseEntity.ok(resp);
    }
}

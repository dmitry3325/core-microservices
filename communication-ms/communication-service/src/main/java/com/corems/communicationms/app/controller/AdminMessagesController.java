package com.corems.communicationms.app.controller;

import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.RequireRoles;
import com.corems.communicationms.api.AdminMessagesApi;
import com.corems.communicationms.api.model.GetMessagesForUser;
import com.corems.communicationms.api.model.MessageRequest;
import com.corems.communicationms.api.model.MessageResponse;
import com.corems.communicationms.app.service.MessagingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequireRoles(CoreMsRoles.COMMUNICATION_MS_ADMIN)
public class AdminMessagesController implements AdminMessagesApi {

    private final MessagingService messagingService;

    @Autowired
    public AdminMessagesController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public ResponseEntity<GetMessagesForUser> getAdminMessages(
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> sort,
            Optional<String> search,
            Optional<List<String>> filter,
            Optional<String> userId) {

        GetMessagesForUser resp = this.messagingService.listMessages(
                userId.orElse(null),
                page,
                pageSize,
                sort,
                search,
                filter
        );

        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<MessageResponse> sendAdminMessage(@Valid MessageRequest messageRequest) {
        if (messageRequest.getUserId() == null || messageRequest.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId is required when sending a message as admin");
        }

        MessageResponse result = this.messagingService.sendMessage(messageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}


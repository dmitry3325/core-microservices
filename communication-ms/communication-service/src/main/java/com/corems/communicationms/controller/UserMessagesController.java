package com.corems.communicationms.controller;

import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.RequireRoles;
import com.corems.communicationms.api.UserMessagesApi;
import com.corems.communicationms.model.GetMessagesForUser;
import com.corems.communicationms.model.MessageRequest;
import com.corems.communicationms.model.MessageResponse;
import com.corems.communicationms.service.MessagingService;
import com.corems.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
public class UserMessagesController implements UserMessagesApi {
    private final MessagingService messagingService;

    @Autowired
    public UserMessagesController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public ResponseEntity<GetMessagesForUser> getMyMessages(
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> sort,
            Optional<String> search,
            Optional<List<String>> filter,
            Optional<String> type) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        GetMessagesForUser resp = this.messagingService.listMessages(
                userPrincipal.getUserId(),
                page,
                pageSize,
                sort,
                search,
                filter
        );

        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<MessageResponse> sendMyMessage(@Valid MessageRequest messageRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        messageRequest.setUserId(userPrincipal.getUserId());

        MessageResponse result = this.messagingService.sendMessage(messageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}


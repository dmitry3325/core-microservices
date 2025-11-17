package com.corems.communicationms.app.controller;

import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.RequireRoles;
import com.corems.common.security.UserPrincipal;
import com.corems.communicationms.api.MessagesApi;
import com.corems.communicationms.api.model.EmailMessageRequest;
import com.corems.communicationms.api.model.MessageListResponse;
import com.corems.communicationms.api.model.MessageResponse;
import com.corems.communicationms.api.model.SmsMessageRequest;
import com.corems.communicationms.app.service.EmailService;
import com.corems.communicationms.app.service.MessagingService;
import com.corems.communicationms.app.service.SmsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
public class MessagesController implements MessagesApi {
    private final MessagingService messagingService;
    private final EmailService emailService;
    private final SmsService smsService;

    @Autowired
    public MessagesController(MessagingService messagingService,
                              EmailService emailService,
                              SmsService smsService) {
        this.messagingService = messagingService;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @Override
    public ResponseEntity<MessageListResponse> getMessages(
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> sort,
            Optional<String> search,
            Optional<List<String>> filter) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + CoreMsRoles.COMMUNICATION_MS_ADMIN.name()));

        String userScope = isAdmin ? null : userPrincipal.getUserId();

        MessageListResponse resp = this.messagingService.listMessages(
                userScope,
                page,
                pageSize,
                sort,
                search,
                filter
        );

        return ResponseEntity.ok(resp);
    }

    @Override
    @RequireRoles(CoreMsRoles.COMMUNICATION_MS_ADMIN)
    public ResponseEntity<MessageResponse> sendSmsMessage(@Valid SmsMessageRequest smsMessageRequest) {
        log.info("Received SMS message request: {}", smsMessageRequest);
        MessageResponse response = smsService.sendMessage(smsMessageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @RequireRoles(CoreMsRoles.COMMUNICATION_MS_ADMIN)
    public ResponseEntity<MessageResponse> sendEmailMessage(@Valid EmailMessageRequest emailMessageRequest) {
        log.info("Received Email message request: {}", emailMessageRequest);
        MessageResponse response = emailService.sendMessage(emailMessageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

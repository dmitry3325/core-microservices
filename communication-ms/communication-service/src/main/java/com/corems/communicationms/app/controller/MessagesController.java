package com.corems.communicationms.app.controller;

import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.RequireRoles;
import com.corems.common.security.SecurityUtils;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MessagesController implements MessagesApi {
    private final MessagingService messagingService;
    private final EmailService emailService;
    private final SmsService smsService;

    @Override
    public ResponseEntity<MessageListResponse> getMessages(
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> sort,
            Optional<String> search,
            Optional<List<String>> filter) {

        UserPrincipal userPrincipal = SecurityUtils.getUserPrincipal();

        EnumSet<CoreMsRoles> privileged = EnumSet.copyOf(CoreMsRoles.getSystemRoles());
        privileged.add(CoreMsRoles.COMMUNICATION_MS_ADMIN);

        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(granted -> {
                    try {
                        CoreMsRoles role = CoreMsRoles.valueOf(granted.getAuthority());
                        return privileged.contains(role);
                    } catch (IllegalArgumentException | NullPointerException ex) {
                        return false;
                    }
                });

        UUID userScope = isAdmin ? null : userPrincipal.getUserId();

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

package com.corems.communicationms.service.provider;

import com.corems.common.security.UserPrincipal;
import com.corems.common.service.exception.ServiceException;
import com.corems.common.service.exception.handler.DefaultExceptionReasonCodes;
import com.corems.communicationms.entity.EmailMessageEntity;
import com.corems.communicationms.entity.MessageEntity;
import com.corems.communicationms.model.EmailRequest;
import com.corems.communicationms.model.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Component
public class EmailServiceProvider extends MessagingServiceProvider<EmailMessageEntity, EmailRequest> {

    private final JavaMailSender mailSender;
    private final String defaultFrom;

    @Autowired
    public EmailServiceProvider(JavaMailSender mailSender,
                                @Value("${mail.from:${EMAIL_FROM:no-reply@example.com}}") String defaultFrom) {
        this.mailSender = mailSender;
        this.defaultFrom = defaultFrom;
    }

    @Override
    public void validate(EmailRequest messageRequest) {
        if (messageRequest.getRecipient() == null || messageRequest.getRecipient().isBlank()) {
            throw new IllegalArgumentException("Email recipient is required");
        }
    }

    @Override
    protected EmailMessageEntity createEntityAndSave(EmailRequest emailRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        EmailMessageEntity emailEntity = new EmailMessageEntity();
        emailEntity.setUuid(UUID.randomUUID().toString());
        emailEntity.setEmailType(emailRequest.getEmailType() == null ? "TXT" : emailRequest.getEmailType().toString());
        emailEntity.setSubject(emailRequest.getSubject());
        emailEntity.setSender(emailRequest.getSender() == null ? defaultFrom : emailRequest.getSender());
        emailEntity.setRecipient(emailRequest.getRecipient());
        emailEntity.setBody(emailRequest.getBody());
        emailEntity.setUserId(userPrincipal.getUserId());
        emailEntity.setCreatedAt(Instant.now());
        emailEntity.setStatus(MessageEntity.MessageStatus.CREATED);

        messageRepository.save(emailEntity);
        return emailEntity;
    }

    @Override
    public EmailMessageEntity sendDirect(EmailMessageEntity entity) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(entity.getSender() == null ? defaultFrom : entity.getSender());
            helper.setTo(entity.getRecipient());
            helper.setSubject(entity.getSubject() == null ? "(no-subject)" : entity.getSubject());
            boolean isHtml = entity.getEmailType() != null && entity.getEmailType().equalsIgnoreCase("HTML");
            helper.setText(entity.getBody() == null ? "" : entity.getBody(), isHtml);

            mailSender.send(mime);

            entity.setStatus(MessageEntity.MessageStatus.SENT);
            entity.setUpdatedAt(Instant.now());
            messageRepository.save(entity);

            return entity;
        } catch (Exception ex) {
            entity.setStatus(MessageEntity.MessageStatus.FAILED);
            entity.setUpdatedAt(Instant.now());
            messageRepository.save(entity);

            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Unable to send email message.");
        }
    }
}

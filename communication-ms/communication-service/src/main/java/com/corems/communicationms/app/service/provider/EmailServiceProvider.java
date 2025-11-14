package com.corems.communicationms.app.service.provider;

import com.corems.common.security.UserPrincipal;
import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.communicationms.app.config.MailConfig;
import com.corems.communicationms.app.entity.EmailMessageEntity;
import com.corems.communicationms.app.entity.MessageEntity;
import com.corems.communicationms.api.model.EmailRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final MailConfig config;

    @Autowired
    public EmailServiceProvider(MailConfig config,
                                JavaMailSender mailSender) {
        this.config = config;
        this.mailSender = mailSender;
    }

    @Override
    protected EmailMessageEntity createEntityAndSave(EmailRequest emailRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        EmailMessageEntity emailEntity = new EmailMessageEntity();
        emailEntity.setUuid(UUID.randomUUID().toString());
        emailEntity.setEmailType(emailRequest.getEmailType() == null ? "TXT" : emailRequest.getEmailType().toString());
        emailEntity.setSubject(emailRequest.getSubject());
        emailEntity.setSender(emailRequest.getSender() == null ? config.getDefaultFrom() : emailRequest.getSender());
        emailEntity.setSenderName(emailRequest.getSenderName());
        if (emailRequest.getCc()!= null && !emailRequest.getCc().isEmpty()) {
            emailEntity.setCc(String.join(",", emailRequest.getCc()));
        }
        if (emailRequest.getBcc()!= null && !emailRequest.getBcc().isEmpty()) {
            emailEntity.setBcc(String.join(",", emailRequest.getBcc()));
        }
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
            if (!config.getEnabled()) {
                log.info("Email sending disabled! Simulating send to {}: {}", entity.getRecipient(), entity.getBody());

                entity.setStatus(MessageEntity.MessageStatus.SENT);
                entity.setUpdatedAt(Instant.now());
                messageRepository.save(entity);
                return entity;
            }

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(entity.getSender() == null ? config.getDefaultFrom() : entity.getSender(), entity.getSenderName());
            helper.setTo(entity.getRecipient());
            helper.setSubject(entity.getSubject() == null ? "No reply" : entity.getSubject());
            boolean isHtml = entity.getEmailType() != null && entity.getEmailType().equalsIgnoreCase("HTML");
            helper.setText(entity.getBody() == null ? "" : entity.getBody(), isHtml);

            if (!Strings.isEmpty(entity.getCc())) helper.setBcc(entity.getCc());
            if (!Strings.isEmpty(entity.getBcc())) helper.setBcc(entity.getBcc());

            mailSender.send(mime);

            entity.setStatus(MessageEntity.MessageStatus.SENT);
            entity.setUpdatedAt(Instant.now());
            messageRepository.save(entity);

            return entity;
        } catch (Exception ex) {
            entity.setStatus(MessageEntity.MessageStatus.FAILED);
            entity.setUpdatedAt(Instant.now());
            messageRepository.save(entity);

            log.error("Failed to send email message: {}", ex.getMessage());
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Unable to send email message.");
        }
    }
}

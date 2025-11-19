package com.corems.communicationms.app.service.provider;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.communicationms.api.model.EmailPayload;
import com.corems.communicationms.app.config.MailConfig;
import com.corems.communicationms.app.model.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceProvider implements ChannelProvider<EmailPayload> {
    private final JavaMailSender mailSender;
    private final MailConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public MessageType getMessageType() {
        return MessageType.email;
    }

    @Override
    public void convertAndSend(Object payload) {
        send(objectMapper.convertValue(payload, EmailPayload.class));
    }

    @Override
    public void send(EmailPayload payload) {
        if (!config.getEnabled()) {
            log.info("Email sending disabled! Simulating send to {}: {}", payload.getRecipient(), payload.getBody());
            return;
        }

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(payload.getSender() == null ? config.getDefaultFrom() : payload.getSender(), payload.getSenderName());
            helper.setTo(payload.getRecipient());
            helper.setSubject(payload.getSubject() == null ? "No reply" : payload.getSubject());
            boolean isHtml = payload.getEmailType() != null && payload.getEmailType().equals(EmailPayload.EmailTypeEnum.HTML);
            helper.setText(payload.getBody() == null ? "" : payload.getBody(), isHtml);

            if (payload.getCc() != null && !payload.getCc().isEmpty()) helper.setBcc(payload.getCc().toArray(String[]::new));
            if (payload.getBcc() != null && !payload.getBcc().isEmpty()) helper.setBcc(payload.getBcc().toArray(String[]::new));

            mailSender.send(mime);
        } catch (Exception ex) {
            log.error("Failed to send email message: {}", ex.getMessage());
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Unable to send email message.");
        }
    }
}

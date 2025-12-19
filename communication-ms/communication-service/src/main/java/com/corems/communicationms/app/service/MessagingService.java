package com.corems.communicationms.app.service;

import com.corems.communicationms.api.model.ChannelType;
import com.corems.communicationms.api.model.EmailPayload;
import com.corems.communicationms.api.model.SendStatus;
import com.corems.communicationms.api.model.SmsPayload;
import com.corems.communicationms.app.entity.EmailMessageEntity;
import com.corems.communicationms.app.entity.MessageEntity;
import com.corems.communicationms.api.model.MessageResponse;
import com.corems.communicationms.api.model.MessageListResponse;
import com.corems.communicationms.app.entity.SMSMessageEntity;
import com.corems.communicationms.app.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import com.corems.common.utils.db.utils.QueryParams;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingService {

    private final MessageRepository messageRepository;

    public MessageListResponse listMessages(
            UUID userId,
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> sort,
            Optional<String> search,
            Optional<List<String>> filter
    ) {
        // Merge userId constraint into filters so the executor will scope to this user
        List<String> mergedFilters = new java.util.ArrayList<>();
        if (userId != null) {
            mergedFilters.add("userId:eq:" + userId);
        }
        filter.ifPresent(mergedFilters::addAll);

        // If no sort provided, default to createdAt desc
        sort = sort.or(() -> Optional.of("createdAt:desc"));

        QueryParams params = new QueryParams(page, pageSize, search, sort, Optional.of(mergedFilters));
        Page<MessageEntity> result = messageRepository.findAllByQueryParams(params);

        List<MessageResponse> items = result.getContent().stream()
                .map(this::mapToMessageResponse)
                .toList();

        MessageListResponse response = new MessageListResponse(result.getNumber() + 1, result.getSize());
        response.setItems(items);
        response.setTotalPages(result.getTotalPages());
        response.setTotalElements(result.getTotalElements());

        return response;
    }

    private MessageResponse mapToMessageResponse(MessageEntity entity) {
        MessageResponse mr = new MessageResponse();
        mr.setUuid(entity.getUuid());
        mr.setType(entity.getType() == null ? null : ChannelType.fromValue(entity.getType().toString()));
        mr.setStatus(SendStatus.fromValue(entity.getStatus().toString()));
        mr.createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        mr.setUserId(entity.getUserId());
        mr.setSentById(entity.getSentById());
        if (entity.getSentByType() != null) {
            mr.setSentByType(MessageResponse.SentByTypeEnum.fromValue(entity.getSentByType().name()));
        }

        if (entity.getType() == null) {
            log.error("Message entity has null type for uuid={}", entity.getUuid());
            return mr;
        }

        switch (entity.getType()) {
            case email -> mr.setPayload(mapEmail((EmailMessageEntity) entity));
            case sms -> mr.setPayload(mapSms((SMSMessageEntity) entity));
            default -> log.error("Missing error mapper for type={}, uuid={}", entity.getType(), entity.getUuid());
        }

        return mr;
    }

    public EmailPayload mapEmail(EmailMessageEntity emailEntity) {
        EmailPayload ep = new EmailPayload(
                ChannelType.EMAIL.getValue(),
                emailEntity.getSubject(),
                emailEntity.getRecipient(),
                emailEntity.getBody()
        );
        if (emailEntity.getEmailType() != null) {
            try {
                ep.setEmailType(EmailPayload.EmailTypeEnum.fromValue(emailEntity.getEmailType()));
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown emailType '{}' for message uuid={}", emailEntity.getEmailType(), emailEntity.getUuid());
            }
        }
        if (emailEntity.getSender() != null) ep.setSender(emailEntity.getSender());
        if (emailEntity.getSenderName() != null) ep.setSenderName(emailEntity.getSenderName());
        if (emailEntity.getCc() != null && !emailEntity.getCc().isBlank()) {
            List<String> cc = java.util.Arrays.stream(emailEntity.getCc().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            ep.setCc(cc);
        }
        if (emailEntity.getBcc() != null && !emailEntity.getBcc().isBlank()) {
            List<String> bcc = java.util.Arrays.stream(emailEntity.getBcc().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            ep.setBcc(bcc);
        }
        return ep;
    }

    public SmsPayload mapSms(SMSMessageEntity smsEntity) {
        return new SmsPayload(ChannelType.SMS.getValue(), smsEntity.getPhoneNumber(), smsEntity.getMessage());
    }
}

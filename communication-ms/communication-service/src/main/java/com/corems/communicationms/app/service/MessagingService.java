package com.corems.communicationms.app.service;

import com.corems.common.queue.QueueProvider;
import com.corems.communicationms.api.model.ChannelType;
import com.corems.communicationms.api.model.SendStatus;
import com.corems.communicationms.app.entity.MessageEntity;
import com.corems.communicationms.api.model.MessageResponse;
import com.corems.communicationms.api.model.MessageListResponse;
import com.corems.communicationms.api.model.PaginationMeta;
import com.corems.communicationms.app.repository.MessageRepository;
import com.corems.communicationms.app.service.provider.EmailServiceProvider;
import com.corems.communicationms.app.service.provider.SmsServiceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import com.corems.common.utils.db.utils.QueryParams;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessagingService {

    private final MessageRepository messageRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    private final static List<ChannelType> SUPPORTED_TYPES = List.of(ChannelType.EMAIL, ChannelType.SMS);
    private final static String QUEUE_NAME = "messages";

    @Autowired
    public MessagingService(
            QueueProvider queueProvider,
            MessageRepository messageRepository,
            EmailServiceProvider emailServiceProvider,
            SmsServiceProvider smsServiceProvider
    ) {
        this.messageRepository = messageRepository;
    }

    public MessageListResponse listMessages(
            String userId,
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> sort,
            Optional<String> search,
            Optional<List<String>> filter
    ) {
        // Merge userId constraint into filters so the executor will scope to this user
        List<String> mergedFilters = new java.util.ArrayList<>();
        // Only add user constraint when a userId is provided (for user-scoped listing)
        if (userId != null && !userId.isBlank()) {
            mergedFilters.add("userId:eq:" + userId);
        }
        filter.ifPresent(mergedFilters::addAll);

        // Build QueryParams with controller-provided optionals (page/pageSize/search/sort/filter)
        QueryParams params = new QueryParams(page, pageSize, search, sort, Optional.of(mergedFilters));

        Page<MessageEntity> result = messageRepository.findAllByQueryParams(params);

        // Build list response
        PaginationMeta meta = new PaginationMeta(result.getNumber() + 1, result.getSize());
        meta.setTotalElements(result.getTotalElements());
        meta.setTotalPages(result.getTotalPages());

        List<MessageResponse> items = result.getContent().stream().map(entity -> {
            MessageResponse mr = new MessageResponse();
            mr.setUuid(entity.getUuid());
            mr.setType(entity.getType() == null ? null : ChannelType.valueOf(entity.getType().toString()));
            mr.setStatus(SendStatus.valueOf(entity.getStatus().toString()));
            mr.createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));


            return mr;
        }).collect(Collectors.toList());

        MessageListResponse response = new MessageListResponse();
        response.setMeta(meta);
        response.setData(items);
        return response;
    }
}

package com.corems.communicationms.service;

import com.corems.communicationms.entity.EmailMessageEntity;
import com.corems.communicationms.entity.MessageEntity;
import com.corems.communicationms.entity.SMSMessageEntity;
import com.corems.communicationms.model.MessageRequest;
import com.corems.communicationms.model.MessageResponse;
import com.corems.communicationms.model.GetMessagesForUser;
import com.corems.communicationms.repository.MessageRepository;
import com.corems.communicationms.service.provider.SlackServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import com.corems.common.utils.db.utils.QueryParams;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessagingService {

    private final MessageRepository messageRepository;

    private final SlackServiceProvider slackServiceProvider;

    public MessagingService(
            MessageRepository notificationRepository,
            SlackServiceProvider slackServiceProvider
    ) {
        this.messageRepository = notificationRepository;
        this.slackServiceProvider = slackServiceProvider;
    }

    public MessageResponse sendMessage(MessageRequest message) {
        MessageEntity notificationEntity = switch (message.getType()) {
            case SMS -> new SMSMessageEntity();
            case SLACK -> slackServiceProvider.sendMessage(message);
            case EMAIL -> new EmailMessageEntity();
        };

        messageRepository.save(notificationEntity);

        MessageResponse response = new MessageResponse();
        response.setId(notificationEntity.getId().toString());
        response.setType(notificationEntity.getType().getValue());
        response.createdAt(notificationEntity.getCreatedAt().atOffset(ZoneOffset.UTC));
        response.setData(message);

        return response;
    }

    public GetMessagesForUser listMessages(
            String userId,
            Optional<String> type,
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> sort,
            Optional<String> search,
            Optional<List<String>> filter
    ) {
        // Merge userId constraint into filters so the executor will scope to this user
        List<String> mergedFilters = new java.util.ArrayList<>();
        mergedFilters.add("userId:eq:" + userId);
        filter.ifPresent(mergedFilters::addAll);

        // Build QueryParams with controller-provided optionals (page/pageSize/search/sort/filter)
        QueryParams params = new QueryParams(page, pageSize, search, sort, Optional.of(mergedFilters));

        Page<MessageEntity> result = messageRepository.findAllByQueryParams(params);

        GetMessagesForUser messages = new GetMessagesForUser();
        messages.setTotal((int) result.getTotalElements());
        messages.setPage(result.getNumber() + 1); // repository returns 0-based; API is 1-based
        messages.setPageSize(result.getSize());

        List<MessageResponse> items = result.getContent().stream().map(entity -> {
            MessageResponse mr = new MessageResponse();
            mr.setId(entity.getId() == null ? null : entity.getId().toString());
            mr.setType(entity.getType() == null ? null : entity.getType().getValue());
            mr.createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
            // data: put minimal payload info (we don't expose entity directly)
            // For now, set data to null to avoid exposing internal entity structure. If needed, map properly.
            mr.setData(null);
            return mr;
        }).collect(Collectors.toList());

        messages.setItems(items);
        return messages;
    }
}
package com.corems.communicationms.service;

import com.corems.common.service.exception.ServiceException;
import com.corems.common.service.exception.handler.DefaultExceptionReasonCodes;
import com.corems.communicationms.entity.MessageEntity;
import com.corems.communicationms.model.EmailRequest;
import com.corems.communicationms.model.MessageRequest;
import com.corems.communicationms.model.MessageResponse;
import com.corems.communicationms.model.GetMessagesForUser;
import com.corems.communicationms.model.SlackRequest;
import com.corems.communicationms.repository.MessageRepository;
import com.corems.communicationms.service.provider.SlackServiceProvider;
import com.corems.communicationms.service.provider.EmailServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import com.corems.common.utils.db.utils.QueryParams;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessagingService {

    private final MessageRepository messageRepository;

    private final SlackServiceProvider slackServiceProvider;
    private final EmailServiceProvider emailServiceProvider;

    @Autowired
    public MessagingService(
            MessageRepository messageRepository,
            SlackServiceProvider slackServiceProvider,
            EmailServiceProvider emailServiceProvider
    ) {
        this.messageRepository = messageRepository;
        this.slackServiceProvider = slackServiceProvider;
        this.emailServiceProvider = emailServiceProvider;
    }

    public MessageResponse sendMessage(MessageRequest message) {
        MessageEntity messageEntity;
        switch (message.getType()) {
            case SLACK -> {
                slackServiceProvider.validate((SlackRequest) message);
                messageEntity = slackServiceProvider.sendMessage((SlackRequest) message);
            }
            case EMAIL -> {
                emailServiceProvider.validate((EmailRequest) message);
                messageEntity = emailServiceProvider.sendMessage((EmailRequest) message);
            }
            case SMS -> throw ServiceException.of(DefaultExceptionReasonCodes.NOT_IMPLEMENTED, "SMS provider not implemented yet");
            default -> throw ServiceException.of(DefaultExceptionReasonCodes.PARAMETER_INVALID, "Unexpected message type: " + message.getType());
        }

        MessageResponse response = new MessageResponse();
        response.setId(messageEntity.getUuid());
        response.setType(messageEntity.getType().getValue());
        response.setStatus(messageEntity.getStatus().toString());
        response.createdAt(messageEntity.getCreatedAt().atOffset(ZoneOffset.UTC));
        response.setData(message);

        return response;
    }

    public GetMessagesForUser listMessages(
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
            mr.setStatus(entity.getStatus() == null ? null : entity.getStatus().toString());
            return mr;
        }).collect(Collectors.toList());

        messages.setItems(items);
        return messages;
    }
}

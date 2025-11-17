package com.corems.communicationms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.common.queue.QueueClient;
import com.corems.common.queue.QueueMessage;
import com.corems.common.queue.QueueProvider;
import com.corems.communicationms.app.model.MessageStatus;
import com.corems.communicationms.app.model.MessageType;
import com.corems.communicationms.app.service.provider.ChannelProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageDispatcher {
    private final QueueProvider queueProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> MessageStatus dispatchMessage(ChannelProvider<T> channelProvider, MessageType type, T payload) throws ServiceException {
        if (queueProvider.isEnabled()) {
            QueueClient queueClient = queueProvider.getDefaultClient();

            QueueMessage<Object> qm = new QueueMessage<>();
            qm.setId(UUID.randomUUID().toString());
            qm.setType(type.toString());

            try {
                qm.setPayload(objectMapper.writeValueAsString(payload));
            } catch (JsonProcessingException e) {
                throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR);
            }

            queueClient.send(qm);
            return MessageStatus.enqueued;
        }

        channelProvider.send(payload);
        return MessageStatus.sent;
    }
}

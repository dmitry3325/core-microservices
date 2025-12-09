package com.corems.communicationms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.queue.QueueClient;
import com.corems.common.queue.QueueMessage;
import com.corems.common.queue.QueueProvider;
import com.corems.communicationms.app.model.MessageStatus;
import com.corems.communicationms.app.service.provider.ChannelProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageDispatcher {
    private final QueueProvider queueProvider;

    public <T> MessageStatus dispatchMessage(ChannelProvider<T> channelProvider, UUID messageId, T payload) throws ServiceException {
        if (queueProvider.isEnabled()) {
            QueueClient queueClient = queueProvider.getDefaultClient();

            QueueMessage qm = new QueueMessage();
            qm.setId(messageId.toString());
            qm.setType(channelProvider.getMessageType().toString());
            qm.setPayload(payload);
            queueClient.send(qm);
            return MessageStatus.enqueued;
        }

        channelProvider.send(payload);
        return MessageStatus.sent;
    }
}

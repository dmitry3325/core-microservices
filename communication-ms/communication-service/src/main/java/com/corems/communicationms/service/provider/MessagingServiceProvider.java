package com.corems.communicationms.service.provider;

import com.corems.common.queue.QueueClient;
import com.corems.common.queue.QueueMessage;
import com.corems.common.queue.QueueProvider;
import com.corems.communicationms.entity.MessageEntity;
import com.corems.communicationms.model.MessageRequest;
import com.corems.communicationms.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

public abstract class MessagingServiceProvider<T extends MessageEntity, U extends MessageRequest> {
    @Autowired(required = false)
    protected MessageRepository messageRepository;

    @Autowired(required = false)
    protected QueueProvider queueProvider;

    public abstract T sendDirect(T messageRequest);
    protected abstract T createEntityAndSave(U messageRequest);

    public T sendMessage(U messageRequest) {
        T entity = createEntityAndSave(messageRequest);

        if (queueProvider.isEnabled()) {
            QueueClient queueClient = queueProvider.getDefaultClient();

            QueueMessage<Object> qm = new QueueMessage<>();
            qm.setId(entity.getUserId());

            queueClient.send(qm);

            messageRepository.findById(entity.getId()).ifPresent(e -> {
                e.setStatus(MessageEntity.MessageStatus.ENQUEUED);
                e.setUpdatedAt(Instant.now());
                messageRepository.save(e);
            });

            return entity;
        }

        return sendDirect(entity);
    }
}

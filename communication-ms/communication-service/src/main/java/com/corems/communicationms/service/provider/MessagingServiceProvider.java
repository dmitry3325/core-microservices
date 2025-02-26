package com.corems.communicationms.service.provider;

import com.corems.communicationms.entity.MessageEntity;
import com.corems.communicationms.model.MessageRequest;

public interface MessagingServiceProvider {
    MessageEntity sendMessage(MessageRequest messageRequest);
}

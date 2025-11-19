package com.corems.communicationms.app.service.provider;

import com.corems.common.exception.ServiceException;
import com.corems.communicationms.app.model.MessageType;

public interface ChannelProvider<T> {
    public MessageType getMessageType();
    void send(T message) throws ServiceException;
    void convertAndSend(Object message) throws ServiceException;
}

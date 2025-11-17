package com.corems.communicationms.app.service.provider;

import com.corems.common.exception.ServiceException;

public interface ChannelProvider<T> {
    void send(T message) throws ServiceException;
}

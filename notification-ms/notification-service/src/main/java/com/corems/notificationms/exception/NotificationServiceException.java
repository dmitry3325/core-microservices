package com.corems.notificationms.exception;

import com.corems.common.error.handler.exceptions.ServiceException;

public class NotificationServiceException extends ServiceException {
    public NotificationServiceException(String message) {
        super(message);
    }

    public NotificationServiceException(String reasonCode, String message) {
        super(reasonCode, message);
    }
}

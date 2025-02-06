package com.corems.userms.exception;

import com.corems.common.service.exception.ServiceException;
import com.corems.common.service.exception.handler.ExceptionReasonCodes;

public class UserServiceException extends ServiceException {
    public UserServiceException(ExceptionReasonCodes reasonCode) {
        super(reasonCode);
    }

    public UserServiceException(ExceptionReasonCodes reasonCode, String details) {
        super(reasonCode, details);
    }

    public UserServiceException(String message, String details) {
        super(message, details);
    }

    public UserServiceException(String message) {
        super(message);
    }
}

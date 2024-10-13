package com.corems.userms.model.exception;

import com.corems.common.service.exception.ServiceException;
import com.corems.common.service.exception.handler.ExceptionReasonCodes;

public class AuthServiceException extends ServiceException {
    public AuthServiceException(ExceptionReasonCodes reasonCode) {
        super(reasonCode);
    }

    public AuthServiceException(ExceptionReasonCodes reasonCode, String details) {
        super(reasonCode, details);
    }
}

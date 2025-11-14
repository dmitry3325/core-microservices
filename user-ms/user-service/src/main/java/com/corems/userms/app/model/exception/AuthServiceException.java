package com.corems.userms.app.model.exception;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.ExceptionReasonCodes;

public class AuthServiceException extends ServiceException {
    public AuthServiceException(ExceptionReasonCodes reasonCode) {
        super(reasonCode);
    }

    public AuthServiceException(ExceptionReasonCodes reasonCode, String details) {
        super(reasonCode, details);
    }
}

package com.corems.authms.controller.model.exception;

import com.corems.common.error.handler.exceptions.ServiceException;
import com.corems.common.error.handler.handler.ExceptionReasonCodes;

public class AuthServiceException extends ServiceException {
    public AuthServiceException(ExceptionReasonCodes reasonCode) {
        super(reasonCode);
    }

    public AuthServiceException(ExceptionReasonCodes reasonCode, String details) {
        super(reasonCode, details);
    }
}

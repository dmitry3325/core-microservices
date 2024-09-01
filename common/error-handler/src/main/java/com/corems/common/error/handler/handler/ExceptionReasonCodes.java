package com.corems.common.error.handler.handler;

import org.springframework.http.HttpStatus;

public interface ExceptionReasonCodes {
    String getErrorCode();
    HttpStatus getHttpStatus();
    String getDescription();
}

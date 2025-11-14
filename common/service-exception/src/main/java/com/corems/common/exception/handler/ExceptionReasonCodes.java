package com.corems.common.exception.handler;

import org.springframework.http.HttpStatus;

public interface ExceptionReasonCodes {
    String getErrorCode();
    HttpStatus getHttpStatus();
    String getDescription();
}

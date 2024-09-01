package com.corems.common.error.handler.exceptions;

import com.corems.common.error.handler.handler.DefaultExceptionReasonCodes;
import com.corems.common.error.handler.models.Error;
import com.corems.common.error.handler.handler.ExceptionReasonCodes;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ToString
public abstract class ServiceException extends RuntimeException {

    private final transient List<Error> errors = new ArrayList<>();

    private final HttpStatus httpStatusCode;

    public ServiceException(ExceptionReasonCodes reasonCode) {
        super(reasonCode.getDescription());
        this.httpStatusCode = reasonCode.getHttpStatus();
        this.errors.add(Error.of(reasonCode.getErrorCode(), reasonCode.getDescription(), null));
    }

    public ServiceException(ExceptionReasonCodes reasonCode, String details) {
        super(reasonCode.getDescription());
        this.httpStatusCode = reasonCode.getHttpStatus();
        this.errors.add(Error.of(reasonCode.getErrorCode(), reasonCode.getDescription(), details));
    }

    public ServiceException(String message, String details) {
        super(message);
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errors.add(Error.of(DefaultExceptionReasonCodes.SERVER_ERROR.getErrorCode(), message, details));
    }

    public ServiceException(String message) {
        super(message);
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errors.add(Error.of(DefaultExceptionReasonCodes.SERVER_ERROR.getErrorCode(), message));
    }

    public ServiceException(List<Error> errors, HttpStatus httpStatusCode) {
        super(errors.get(0).getDescription());
        this.httpStatusCode = httpStatusCode;
        this.errors.addAll(errors);
    }

    public HttpStatus getHttpStatusCode() {
        return this.httpStatusCode;
    }

    public List<Error> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}

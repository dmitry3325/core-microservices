package com.corems.common.exception;

import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.common.exception.model.Error;
import com.corems.common.exception.handler.ExceptionReasonCodes;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ToString
public class ServiceException extends RuntimeException {

    private final transient List<Error> errors = new ArrayList<>();

    private final HttpStatus httpStatusCode;

    public static ServiceException of(ExceptionReasonCodes reasonCode) {
        return new ServiceException(reasonCode);
    }

    public static ServiceException of(ExceptionReasonCodes reasonCode, String details) {
        return new ServiceException(reasonCode, details);
    }

    public ServiceException() {
        super(DefaultExceptionReasonCodes.SERVER_ERROR.getDescription());
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errors.add(Error.of(DefaultExceptionReasonCodes.SERVER_ERROR.getErrorCode(), DefaultExceptionReasonCodes.SERVER_ERROR.getDescription(), null));
    }

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

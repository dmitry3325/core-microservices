package com.corems.userms.app.model.exception;

import com.corems.common.exception.handler.ExceptionReasonCodes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
@ToString
public enum AuthExceptionReasonCodes implements ExceptionReasonCodes {
    UNAUTHORIZED("user.unauthorized", HttpStatus.UNAUTHORIZED, "Unauthorized"),
    USER_NOT_FOUND("user.not_found", HttpStatus.BAD_REQUEST, "User not found"),
    USER_EXISTS("user.not_found", HttpStatus.BAD_REQUEST, "User already exists"),
    USER_PROVIDER_MISMATCH("user.provider_mismatch", HttpStatus.BAD_REQUEST, "User provider mismatch"),
    USER_PASSWORD_MISMATCH("user.password_mismatch", HttpStatus.BAD_REQUEST, "User password mismatch"),
    PROVIDER_IS_NOT_SUPPORTED("provider.is_not_supported", HttpStatus.BAD_REQUEST, "Provider is not supported");

    private final String errorCode;

    private final HttpStatus httpStatus;

    private final String description;
}

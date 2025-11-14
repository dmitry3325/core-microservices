package com.corems.userms.app.exception;

import com.corems.common.exception.handler.ExceptionReasonCodes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
@ToString
public enum UserServiceExceptionReasonCodes implements ExceptionReasonCodes {

    USER_EXISTS("user.exists", HttpStatus.BAD_REQUEST, "User already exists"),
    INVALID_ROLE("user.invalidrole", HttpStatus.BAD_REQUEST, "Invalid role specified"),
    USER_NOT_FOUND("user.notfound", HttpStatus.BAD_REQUEST, "User not found"),
    TOKEN_NOT_FOUND("token.notfound", HttpStatus.BAD_REQUEST, "Token not found. Please login again.");

    private final String errorCode;

    private final HttpStatus httpStatus;

    private final String description;

}

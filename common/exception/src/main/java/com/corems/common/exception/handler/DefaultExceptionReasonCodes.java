package com.corems.common.exception.handler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
@ToString
public enum DefaultExceptionReasonCodes implements ExceptionReasonCodes {

    SERVER_ERROR("server.error", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error. We will fix it as soon as possible. Try again later."),
    NOT_IMPLEMENTED("server.error", HttpStatus.NOT_IMPLEMENTED, "Sorry, this functionality is not implemented yet."),

    UNAUTHORIZED("user.unauthorized", HttpStatus.UNAUTHORIZED, "User is unauthorized"),
    ACCESS_DENIED("user.access_denied", HttpStatus.UNAUTHORIZED, "Access is denied. You have no rights to perform this operation."),
    FORBIDDEN("user.forbidden", HttpStatus.FORBIDDEN, "Access to this resource is forbidden"),

    INVALID_REQUEST("invalid.request", HttpStatus.BAD_REQUEST, "Invalid request"),
    INVALID_INPUT_DATA("invalid.data", HttpStatus.BAD_REQUEST, "Invalid input data"),
    CONFLICT("resource.conflict", HttpStatus.CONFLICT, "Resource conflict detected"),

    PARAMETER_INVALID("parameter.invalid", HttpStatus.BAD_REQUEST, "Parameter invalid"),
    REQUEST_PARAMETER_MISSING("request.parameter.missing", HttpStatus.BAD_REQUEST, "Request parameter is missing"),
    PROVIDED_VALUE_INVALID("provided.value.invalid", HttpStatus.BAD_REQUEST, "Provided value is invalid"),

    HEADER_MISSED("header.missed", HttpStatus.BAD_REQUEST, "Header is missed"),
    FORMAT_INVALID("format.invalid", HttpStatus.BAD_REQUEST, "Format invalid");

    private final String errorCode;

    private final HttpStatus httpStatus;

    private final String description;

}

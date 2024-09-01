package com.corems.common.error.handler.handler;

import com.corems.common.error.handler.exceptions.ServiceException;
import com.corems.common.error.handler.exceptions.StateConflictException;
import com.corems.common.error.handler.models.Error;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class DefaultErrorConverter implements ErrorConverter {

    protected final Comparator<Error> errorComparator;

    public HttpHeaders buildHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Override
    public Error getErrorFromException(Exception ref, WebRequest request) {
        return Error.of(DefaultExceptionReasonCodes.SERVER_ERROR.getErrorCode(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }

    @Override
    public List<Error> getErrorsFromServiceException(ServiceException ex, WebRequest request) {
        return ex.getErrors();
    }

    @Override
    public Error getErrorsFromRuntimeException(RuntimeException ex, WebRequest request) {
        return Error.of(DefaultExceptionReasonCodes.SERVER_ERROR.getErrorCode(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }

    @Override
    public List<Error> getErrorsFromStateConflictException(StateConflictException ex, WebRequest request) {
        return ex.getErrors();
    }

    @Override
    public Error getErrorsFromMissingRequestHeaderException(MissingRequestHeaderException ex, WebRequest request) {
        return Error.of(DefaultExceptionReasonCodes.HEADER_MISSED.getErrorCode(), ex.getHeaderName());
    }

    @Override
    public Error getErrorFromServletRequestBindingException(ServletRequestBindingException ex, WebRequest request) {
        if (ex instanceof MissingRequestHeaderException exception) {
            return getErrorsFromMissingRequestHeaderException(exception, request);
        } else {
            log.error("test");
            return null;
        }
    }

    @Override
    public Error getErrorFromMissingServletRequestParameterException(MissingServletRequestParameterException ex, WebRequest request) {
        return Error.of(
                DefaultExceptionReasonCodes.REQUEST_PARAMETER_MISSING.getErrorCode(),
                DefaultExceptionReasonCodes.REQUEST_PARAMETER_MISSING.getDescription(),
                new MessageFormat("The {0} query string parameter is required, but wasn't provided").format(new String[]{ex.getParameterName()})
        );
    }

    @Override
    public Error getErrorFromTypeMismatchException(TypeMismatchException ex, WebRequest request) {
        if (ex instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException methodEx = (MethodArgumentTypeMismatchException) ex;
            return Error.of(DefaultExceptionReasonCodes.PARAMETER_INVALID.getErrorCode(),
                    DefaultExceptionReasonCodes.PARAMETER_INVALID.getDescription(),
                    new MessageFormat("Parameter {0} invalid").format(new String[]{methodEx.getParameter().getParameterName()})
            );

        }
        return null;
    }

    @Override
    public List<Error> getErrorsFromHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof MismatchedInputException mismatchedInputException) {
            var path = mismatchedInputException.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining("."));

            String msg = "Invalid input data.";
            if (path.length() > 0) {
                msg += new MessageFormat("In {0} property.").format(path);
            }

            return List.of(Error.of(DefaultExceptionReasonCodes.INVALID_REQUEST.getErrorCode(), DefaultExceptionReasonCodes.INVALID_REQUEST.getDescription(), msg));
        }

        return List.of(Error.of(DefaultExceptionReasonCodes.INVALID_REQUEST.getErrorCode(), DefaultExceptionReasonCodes.INVALID_REQUEST.getDescription(), rootCause != null ? rootCause.getMessage() : null));
    }

    @Override
    public Error getErrorFromNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
        return Error.of(DefaultExceptionReasonCodes.INVALID_REQUEST.getErrorCode(), DefaultExceptionReasonCodes.INVALID_REQUEST.getDescription(), ex.getMessage());
    }

    @Override
    public List<Error> getErrorsFromMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        return ex.getBindingResult().getFieldErrors().stream().map(fieldError ->
                Error.of(DefaultExceptionReasonCodes.PROVIDED_VALUE_INVALID.getErrorCode(),
                        DefaultExceptionReasonCodes.PROVIDED_VALUE_INVALID.getDescription(),
                        fieldError.getField() + " - " + fieldError.getDefaultMessage())
        ).sorted(this.errorComparator).collect(Collectors.toList());
    }

    @Override
    public List<Error> getErrorsFromConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        log.error("getErrorsFromConstraintViolationException");
        return null;
    }

}
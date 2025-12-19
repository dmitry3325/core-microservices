package com.corems.common.exception.handler;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.model.Error;
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
    
    @Override
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
        if (ex instanceof MethodArgumentTypeMismatchException methodEx) {
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
            String pathStr = mismatchedInputException.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."))
                    .replace(".[", "["); // make "a.[0].b" -> "a[0].b"

            // Determine whether the last segment is an object property or an array element
            boolean isObjectProperty = !mismatchedInputException.getPath().isEmpty()
                    && mismatchedInputException.getPath()
                    .get(mismatchedInputException.getPath().size() - 1)
                    .getFieldName() != null;

            String msg = "Invalid input data.";
            if (!pathStr.isBlank()) {
                msg += " " + new MessageFormat("In {0} {1}.")
                        .format(new Object[]{pathStr, isObjectProperty ? "property" : "element"});
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
        ).sorted(this.errorComparator).toList();
    }

    @Override
    public List<Error> getErrorsFromConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        if (ex.getConstraintViolations() == null || ex.getConstraintViolations().isEmpty()) {
            return List.of(Error.of(DefaultExceptionReasonCodes.INVALID_INPUT_DATA.getErrorCode(), DefaultExceptionReasonCodes.INVALID_INPUT_DATA.getDescription(), ex.getMessage()));
        }

        return ex.getConstraintViolations().stream().map(cv -> {
            String path = cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : null;
            String message = cv.getMessage();

            String details = "request parameter";
            if (path != null && !path.isBlank()) {
                String last = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;

                // argument nodes often come as arg0, arg1 when validating method parameters
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("arg(\\d+)").matcher(last);
                if (m.matches()) {
                    int idx = Integer.parseInt(m.group(1));
                    // make it 1-based for human readability
                    details = "parameter #" + (idx + 1);
                } else {
                    details = last;
                }
            }

            return Error.of(DefaultExceptionReasonCodes.PROVIDED_VALUE_INVALID.getErrorCode(), DefaultExceptionReasonCodes.PROVIDED_VALUE_INVALID.getDescription(), details + " " + message);
        }).sorted(this.errorComparator).toList();
    }
}
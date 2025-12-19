package com.corems.common.exception.handler;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.model.Error;
import com.corems.common.exception.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class RestServiceExceptionHandler extends ResponseEntityExceptionHandler {

    private final ErrorConverter errorConverter;

    @ExceptionHandler({ServiceException.class})
    protected ResponseEntity<Object> handleServiceException(ServiceException ex, WebRequest request) {
        log.error("ServiceException: ", ex);
        return handleExceptionInternal(ex,
                ErrorResponse.of(errorConverter.getErrorsFromServiceException(ex, request)),
                errorConverter.buildHttpHeaders(), ex.getHttpStatusCode(), request);
    }

    @ExceptionHandler({RuntimeException.class})
    protected ResponseEntity<Object> handleServiceException(RuntimeException ex, WebRequest request) {
        log.error("RuntimeException: ", ex);
        return handleExceptionInternal(ex,
                ErrorResponse.of(errorConverter.getErrorsFromRuntimeException(ex, request)),
                errorConverter.buildHttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        log.error("RuntimeException: ", ex);
        return handleExceptionInternal(ex,
                ErrorResponse.of(errorConverter.getErrorsFromConstraintViolationException(ex, request)),
                errorConverter.buildHttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MultipartException.class)
    protected ResponseEntity<Object> handleMultipartException(MultipartException ex, WebRequest request) {

        Error error = Error.of(DefaultExceptionReasonCodes.INVALID_REQUEST.getErrorCode(), "Invalid multipart request");

        return handleExceptionInternal(ex,
                ErrorResponse.of(error),
                errorConverter.buildHttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Error error = Error.of(DefaultExceptionReasonCodes.INVALID_REQUEST.getErrorCode(), "Maximum upload size exceeded");

        return handleExceptionInternal(ex,
                ErrorResponse.of(error),
                errorConverter.buildHttpHeaders(), HttpStatus.CONTENT_TOO_LARGE, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex,
                ErrorResponse.of(errorConverter.getErrorsFromMethodArgumentNotValidException(ex, request)),
                errorConverter.buildHttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Error error = errorConverter.getErrorFromServletRequestBindingException(ex, request);

        if (error != null) {
            return handleExceptionInternal(ex, ErrorResponse.of(error), errorConverter.buildHttpHeaders(), HttpStatus.BAD_REQUEST, request);
        } else {
            log.error("Unhandled ServletRequestBindingException exception", ex);
            return handleExceptionInternal(ex, null, headers, status, request);
        }
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex,
                ErrorResponse.of(errorConverter.getErrorFromMissingServletRequestParameterException(ex, request)),
                errorConverter.buildHttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> enumResponse = handleEnumTypeMismatch(ex, request);
        if (enumResponse != null) {
            return enumResponse;
        }

        Error error = errorConverter.getErrorFromTypeMismatchException(ex, request);

        if (error != null) {
            return handleExceptionInternal(ex, ErrorResponse.of(error), errorConverter.buildHttpHeaders(), HttpStatus.BAD_REQUEST, request);
        } else {
            log.error("Unhandled TypeMismatch exception", ex);
            return handleExceptionInternal(ex, null, headers, status, request);
        }
    }

    private ResponseEntity<Object> handleEnumTypeMismatch(TypeMismatchException ex, WebRequest request) {
        Class<?> requiredType = ex.getRequiredType();
        Object rejectedValue = ex.getValue();

        if (requiredType == null || !requiredType.isEnum() || !(rejectedValue instanceof String rejected)) {
            return null;
        }

        List<String> allowed = Arrays.stream(requiredType.getEnumConstants())
                .map(Object::toString)
                .toList();

        String message = String.format("Failed to convert value '%s' to enum %s.", rejected, requiredType.getSimpleName());
        String details = String.format("Allowed values: %s", String.join(", ", allowed));

        Error error = Error.of("TYPE_MISMATCH", message, details);
        return handleExceptionInternal(ex, ErrorResponse.of(error), errorConverter.buildHttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex,
                ErrorResponse.of(errorConverter.getErrorsFromHttpMessageNotReadableException(ex, request)),
                errorConverter.buildHttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex,
                ErrorResponse.of(errorConverter.getErrorFromNoHandlerFoundException(ex, request)),
                headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnhandledException(Exception ex, WebRequest request) {
        if (ex instanceof ServiceException) {
            return handleServiceException((ServiceException) ex, request);
        }

        log.error("Unhandled exception: ", ex);
        return handleExceptionInternal(ex, ErrorResponse.of(errorConverter.getErrorFromException(ex, request)),
                errorConverter.buildHttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
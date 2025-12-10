package com.corems.common.exception;

import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.common.exception.handler.ExceptionReasonCodes;
import com.corems.common.exception.model.Error;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceExceptionTest {

    @Test
    void defaultConstructor_CreatesExceptionWithServerError() {
        ServiceException exception = new ServiceException();

        assertEquals(DefaultExceptionReasonCodes.SERVER_ERROR.getDescription(), exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatusCode());
        assertEquals(1, exception.getErrors().size());
        
        Error error = exception.getErrors().get(0);
        assertEquals(DefaultExceptionReasonCodes.SERVER_ERROR.getErrorCode(), error.getReasonCode());
        assertEquals(DefaultExceptionReasonCodes.SERVER_ERROR.getDescription(), error.getDescription());
        assertNull(error.getDetails());
    }

    @Test
    void constructorWithReasonCode_CreatesExceptionWithReasonCode() {
        ServiceException exception = new ServiceException(DefaultExceptionReasonCodes.UNAUTHORIZED);

        assertEquals(DefaultExceptionReasonCodes.UNAUTHORIZED.getDescription(), exception.getMessage());
        assertEquals(DefaultExceptionReasonCodes.UNAUTHORIZED.getHttpStatus(), exception.getHttpStatusCode());
        assertEquals(1, exception.getErrors().size());
        
        Error error = exception.getErrors().get(0);
        assertEquals(DefaultExceptionReasonCodes.UNAUTHORIZED.getErrorCode(), error.getReasonCode());
        assertEquals(DefaultExceptionReasonCodes.UNAUTHORIZED.getDescription(), error.getDescription());
        assertNull(error.getDetails());
    }

    @Test
    void constructorWithReasonCodeAndDetails_CreatesExceptionWithDetails() {
        String details = "Invalid credentials provided";
        ServiceException exception = new ServiceException(DefaultExceptionReasonCodes.UNAUTHORIZED, details);

        assertEquals(DefaultExceptionReasonCodes.UNAUTHORIZED.getDescription(), exception.getMessage());
        assertEquals(DefaultExceptionReasonCodes.UNAUTHORIZED.getHttpStatus(), exception.getHttpStatusCode());
        assertEquals(1, exception.getErrors().size());
        
        Error error = exception.getErrors().get(0);
        assertEquals(DefaultExceptionReasonCodes.UNAUTHORIZED.getErrorCode(), error.getReasonCode());
        assertEquals(DefaultExceptionReasonCodes.UNAUTHORIZED.getDescription(), error.getDescription());
        assertEquals(details, error.getDetails());
    }

    @Test
    void constructorWithMessage_CreatesExceptionWithCustomMessage() {
        String message = "Custom error message";
        ServiceException exception = new ServiceException(message);

        assertEquals(message, exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatusCode());
        assertEquals(1, exception.getErrors().size());
        
        Error error = exception.getErrors().get(0);
        assertEquals(DefaultExceptionReasonCodes.SERVER_ERROR.getErrorCode(), error.getReasonCode());
        assertEquals(message, error.getDescription());
    }

    @Test
    void constructorWithMessageAndDetails_CreatesExceptionWithMessageAndDetails() {
        String message = "Custom error message";
        String details = "Additional error details";
        ServiceException exception = new ServiceException(message, details);

        assertEquals(message, exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatusCode());
        assertEquals(1, exception.getErrors().size());
        
        Error error = exception.getErrors().get(0);
        assertEquals(DefaultExceptionReasonCodes.SERVER_ERROR.getErrorCode(), error.getReasonCode());
        assertEquals(message, error.getDescription());
        assertEquals(details, error.getDetails());
    }

    @Test
    void constructorWithErrorsAndHttpStatus_CreatesExceptionWithMultipleErrors() {
        List<Error> errors = List.of(
            Error.of("ERR001", "First error", "First details"),
            Error.of("ERR002", "Second error", "Second details")
        );
        HttpStatus status = HttpStatus.BAD_REQUEST;
        
        ServiceException exception = new ServiceException(errors, status);

        assertEquals("First error", exception.getMessage()); // Uses first error's description
        assertEquals(status, exception.getHttpStatusCode());
        assertEquals(2, exception.getErrors().size());
        assertEquals(errors, exception.getErrors());
    }

    @Test
    void staticOf_WithReasonCode_CreatesException() {
        ServiceException exception = ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST);

        assertEquals(DefaultExceptionReasonCodes.INVALID_REQUEST.getDescription(), exception.getMessage());
        assertEquals(DefaultExceptionReasonCodes.INVALID_REQUEST.getHttpStatus(), exception.getHttpStatusCode());
    }

    @Test
    void staticOf_WithReasonCodeAndDetails_CreatesException() {
        String details = "Resource with ID 123 not found";
        ServiceException exception = ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST, details);

        assertEquals(DefaultExceptionReasonCodes.INVALID_REQUEST.getDescription(), exception.getMessage());
        assertEquals(DefaultExceptionReasonCodes.INVALID_REQUEST.getHttpStatus(), exception.getHttpStatusCode());
        assertEquals(details, exception.getErrors().get(0).getDetails());
    }

    @Test
    void getErrors_ReturnsUnmodifiableList() {
        ServiceException exception = new ServiceException(DefaultExceptionReasonCodes.INVALID_REQUEST);
        List<Error> errors = exception.getErrors();

        assertThrows(UnsupportedOperationException.class, () -> errors.add(Error.of("TEST", "Test", null)));
    }

    @Test
    void toString_ContainsRelevantInformation() {
        ServiceException exception = ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Test details");
        String toString = exception.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("ServiceException"));
    }

    @Test
    void exceptionWithDifferentReasonCodes_HasCorrectHttpStatuses() {
        ServiceException invalidRequest = ServiceException.of(DefaultExceptionReasonCodes.INVALID_REQUEST);
        ServiceException unauthorized = ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED);
        ServiceException conflict = ServiceException.of(DefaultExceptionReasonCodes.CONFLICT);
        ServiceException serverError = ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR);

        assertEquals(HttpStatus.BAD_REQUEST, invalidRequest.getHttpStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, unauthorized.getHttpStatusCode());
        assertEquals(HttpStatus.CONFLICT, conflict.getHttpStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, serverError.getHttpStatusCode());
    }

    @Test
    void exceptionWithCustomReasonCode_WorksCorrectly() {
        ExceptionReasonCodes customReasonCode = new ExceptionReasonCodes() {
            @Override
            public String getErrorCode() {
                return "CUSTOM_001";
            }

            @Override
            public String getDescription() {
                return "Custom error description";
            }

            @Override
            public HttpStatus getHttpStatus() {
                return HttpStatus.CONFLICT;
            }
        };

        ServiceException exception = ServiceException.of(customReasonCode);

        assertEquals("Custom error description", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatusCode());
        assertEquals("CUSTOM_001", exception.getErrors().get(0).getReasonCode());
    }
}
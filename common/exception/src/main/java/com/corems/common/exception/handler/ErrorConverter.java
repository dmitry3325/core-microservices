package com.corems.common.exception.handler;

import com.corems.common.exception.model.Error;
import com.corems.common.exception.ServiceException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

public interface ErrorConverter {

    HttpHeaders buildHttpHeaders();

    Error getErrorFromException(Exception ref, WebRequest request);

    List<Error> getErrorsFromServiceException(ServiceException ex, WebRequest request);

    Error getErrorsFromRuntimeException(RuntimeException ex, WebRequest request);

    Error getErrorsFromMissingRequestHeaderException(MissingRequestHeaderException ex, WebRequest request);

    Error getErrorFromServletRequestBindingException(ServletRequestBindingException ex, WebRequest request);

    Error getErrorFromMissingServletRequestParameterException(MissingServletRequestParameterException ex, WebRequest request);

    Error getErrorFromTypeMismatchException(TypeMismatchException ex, WebRequest request);

    List<Error> getErrorsFromHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request);

    Error getErrorFromNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request);

    List<Error> getErrorsFromMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request);

    List<Error> getErrorsFromConstraintViolationException(ConstraintViolationException ex, WebRequest request);
}

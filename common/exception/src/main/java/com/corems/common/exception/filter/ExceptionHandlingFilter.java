package com.corems.common.exception.filter;

import com.corems.common.exception.handler.RestServiceExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class ExceptionHandlingFilter extends OncePerRequestFilter {

    private RestServiceExceptionHandler exceptionHandler;
    private ObjectMapper objectMapper = new ObjectMapper();


    public ExceptionHandlingFilter(RestServiceExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request, response);
            RequestContextHolder.setRequestAttributes(requestAttributes);

            ResponseEntity<Object> responseEntity = exceptionHandler.handleUnhandledException(ex, new ServletWebRequest(request));

            response.setStatus(responseEntity.getStatusCode().value());
            response.setContentType("application/json");

            String responseBody = objectMapper.writeValueAsString(responseEntity.getBody());
            response.getWriter().write(responseBody);
        }
    }
}
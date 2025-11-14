package com.corems.userms.app.security.oauth2;

import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class UserAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         org.springframework.security.core.AuthenticationException authException
    ) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write("{\"errors\": [{\"reasonCode\": \"" + DefaultExceptionReasonCodes.UNAUTHORIZED.getErrorCode() + "\",\"description\": \"" + DefaultExceptionReasonCodes.UNAUTHORIZED.getDescription() + "\"}]}");
        response.setStatus(DefaultExceptionReasonCodes.UNAUTHORIZED.getHttpStatus().value());
    }
}

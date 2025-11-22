package com.corems.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.corems.common.security.exception.AuthServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;

public final class SecurityUtils {

    private SecurityUtils() { }

    public static Optional<UserPrincipal> getUserPrincipalOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Optional.empty();
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return Optional.of(up);
        }
        return Optional.empty();
    }

    /**
     * Return current authenticated UserPrincipal or throw an AuthServiceException(UNAUTHORIZED).
     */
    public static UserPrincipal getUserPrincipal() {
        return getUserPrincipalOptional().orElseThrow(() -> AuthServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED));
    }
}

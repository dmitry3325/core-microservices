package com.corems.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.corems.common.exception.ServiceException;
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
        return getUserPrincipalOptional().orElseThrow(() -> ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED));
    }

    /**
     * Check if the current user has the specified role OR is SYSTEM or SUPER_ADMIN.
     * @param role the role to check
     * @return true if user has the role, or is SYSTEM, or is SUPER_ADMIN
     */
    public static boolean hasRole(CoreMsRoles role) {
        return getUserPrincipalOptional()
                .map(up -> up.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> {
                            String authority = grantedAuthority.getAuthority();
                            return authority.equals(role.name())
                                || authority.equals(CoreMsRoles.SYSTEM.name())
                                || authority.equals(CoreMsRoles.SUPER_ADMIN.name());
                        }))
                .orElse(false);
    }
}

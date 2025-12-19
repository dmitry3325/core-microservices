package com.corems.common.security;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class RequireRolesAspect {

    @Around("@within(com.corems.common.security.RequireRoles) || @annotation(com.corems.common.security.RequireRoles)")
    public Object enforceRoles(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();

        RequireRoles annotation = method.getAnnotation(RequireRoles.class);
        if (annotation == null) {
            annotation = pjp.getTarget().getClass().getAnnotation(RequireRoles.class);
        }

        if (annotation == null) {
            return pjp.proceed();
        }

        CoreMsRoles[] requiredRoles = annotation.value();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED);
        }

        Set<String> authorities = auth.getAuthorities().stream()
                .map(a -> {
                    String s = a.getAuthority();
                    if (s.startsWith("ROLE_")) return s.substring(5);
                    return s;
                })
                .collect(Collectors.toSet());

        // Always allow full access to SUPER_ADMIN or SYSTEM
        if (authorities.contains(CoreMsRoles.SUPER_ADMIN.name()) || authorities.contains(CoreMsRoles.SYSTEM.name())) {
            return pjp.proceed();
        }

        boolean allowed = Arrays.stream(requiredRoles)
                .map(CoreMsRoles::name)
                .anyMatch(authorities::contains);

        if (allowed) {
            return pjp.proceed();
        }

        throw ServiceException.of(DefaultExceptionReasonCodes.FORBIDDEN);
    }
}


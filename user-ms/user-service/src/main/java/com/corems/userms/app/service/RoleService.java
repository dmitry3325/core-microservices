package com.corems.userms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.security.CoreMsRoles;
import com.corems.userms.app.config.UserServiceProperties;
import com.corems.userms.app.entity.RoleEntity;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.exception.UserServiceExceptionReasonCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final UserServiceProperties userServiceProperties;

    public void assignRoles(UserEntity user, List<String> desiredRoles) {
        List<String> toAssign;
        if (desiredRoles == null) {
            var configured = userServiceProperties.getDefaultRoles();
            toAssign = (configured == null || configured.isEmpty()) ? List.of("USER") : new ArrayList<>(configured);
        } else {
            toAssign = new ArrayList<>(desiredRoles);
        }

        List<String> normalized = toAssign.stream().map(String::trim).map(String::toUpperCase).toList();

        List<CoreMsRoles> resolved = new ArrayList<>();
        for (String rn : normalized) {
            try {
                resolved.add(resolveRole(rn));
            } catch (IllegalArgumentException _) {
                throw ServiceException.of(UserServiceExceptionReasonCodes.INVALID_ROLE, "Invalid role: " + rn);
            }
        }

        if (user.getRoles() != null) user.getRoles().clear();
        else user.setRoles(new ArrayList<>());

        for (CoreMsRoles roleEnum : resolved) {
            user.getRoles().add(new RoleEntity(roleEnum, user));
        }
    }

    private CoreMsRoles resolveRole(String rn) {
        return CoreMsRoles.valueOf(rn);
    }
}


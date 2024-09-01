package com.corems.authms.service;

import com.corems.authms.controller.model.exception.AuthExceptionReasonCodes;
import com.corems.authms.controller.model.exception.AuthServiceException;
import com.corems.authms.entity.User;
import com.corems.authms.model.UserInfo;
import com.corems.authms.repository.UserRepository;
import com.corems.authms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserInfo getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        return getUserInfo(userPrincipal.getId());

    }

    private UserInfo getUserInfo(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", id)));

        return new UserInfo()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl());
    }
}

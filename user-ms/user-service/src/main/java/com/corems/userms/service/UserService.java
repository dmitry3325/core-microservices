package com.corems.userms.service;

import com.corems.userms.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.model.exception.AuthServiceException;
import com.corems.userms.entity.User;
import com.corems.userms.model.UserInfo;
import com.corems.userms.repository.UserRepository;
import com.corems.userms.security.UserPrincipal;
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

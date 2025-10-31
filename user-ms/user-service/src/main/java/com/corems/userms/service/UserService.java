package com.corems.userms.service;

import com.corems.common.security.UserPrincipal;
import com.corems.userms.model.SuccessfulResponse;
import com.corems.userms.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.model.exception.AuthServiceException;
import com.corems.userms.entity.User;
import com.corems.userms.model.UserInfo;
import com.corems.userms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;


    public UserInfo getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        return getUserInfo(userPrincipal.getUserId());

    }

    public UserInfo getUserById(String userId) {
        // get current user role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();


        return getUserInfo(userId);
    }

    public SuccessfulResponse updateUserById(String userId) {
        return new SuccessfulResponse();
    }

    public List<UserInfo> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserInfo()
                        .userId(user.getUuid())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .imageUrl(user.getImageUrl()))
                .collect(Collectors.toList());
    }

    private UserInfo getUserInfo(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", uuid)));

        return new UserInfo()
                .userId(user.getUuid())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl());
    }
}

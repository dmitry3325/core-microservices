package com.corems.userms.app.service;

import com.corems.common.security.UserPrincipal;
import com.corems.userms.app.entity.Role;
import com.corems.userms.app.entity.User;
import com.corems.userms.api.model.ChangePasswordRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.api.model.UserInfo;
import com.corems.userms.api.model.UserProfileUpdateRequest;
import com.corems.userms.app.model.enums.AuthProvider;
import com.corems.userms.app.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.app.model.exception.AuthServiceException;
import com.corems.userms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserInfo getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        User user = userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userPrincipal.getUserId())));

        log.info(String.valueOf(user.getId()));
        log.info(String.valueOf(user.getLastLogin()));
        log.info(String.valueOf(user.getCreatedAt()));
        log.info(String.valueOf(user.getUpdatedAt()));
        return new UserInfo()
                .userId(user.getUuid())
                .provider(user.getProvider())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .lastLoginAt(user.getLastLogin().atOffset(ZoneOffset.UTC))
                .createdAt(user.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(user.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }

    public UserInfo updateCurrentUserProfile(UserProfileUpdateRequest userProfileUpdateRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userPrincipal.getUserId())));
        if (userProfileUpdateRequest.getFirstName() != null) user.setFirstName(userProfileUpdateRequest.getFirstName());
        if (userProfileUpdateRequest.getLastName() != null) user.setLastName(userProfileUpdateRequest.getLastName());
        if (userProfileUpdateRequest.getImageUrl() != null) user.setImageUrl(userProfileUpdateRequest.getImageUrl().toString());
        userRepository.save(user);
        return new UserInfo()
                .userId(user.getUuid())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl());
    }

    public SuccessfulResponse changeOwnPassword(ChangePasswordRequest changePasswordRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userPrincipal.getUserId())));

        if (user.getPassword() != null && !passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Wrong password");
        }
        if (changePasswordRequest.getNewPassword() == null || changePasswordRequest.getConfirmPassword() == null ||
            !changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "New password and confirm password do not match");
        }

        if (!user.getProvider().contains(AuthProvider.local.name())) {
            user.setProvider(user.getProvider() + "," + AuthProvider.local.name());
        }
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
        return new SuccessfulResponse().result(true);
    }
}

package com.corems.userms.app.service;

import com.corems.common.security.SecurityUtils;
import com.corems.common.security.UserPrincipal;
import com.corems.userms.app.entity.RoleEntity;
import com.corems.userms.app.entity.UserEntity;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    private static final String USER_NOT_FOUND_MSG = "User id: %s not found";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserInfo getCurrentUserInfo() {
        UserPrincipal userPrincipal = SecurityUtils.getUserPrincipal();

        UserEntity user = userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND,
                        String.format(USER_NOT_FOUND_MSG, userPrincipal.getUserId())));

        return mapToUserInfo(user);
    }

    public UserInfo updateCurrentUserProfile(UserProfileUpdateRequest userProfileUpdateRequest) {
        UserPrincipal userPrincipal = SecurityUtils.getUserPrincipal();
        UserEntity user = userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND,
                        String.format(USER_NOT_FOUND_MSG, userPrincipal.getUserId())));
        if (userProfileUpdateRequest.getFirstName() != null)
            user.setFirstName(userProfileUpdateRequest.getFirstName());
        if (userProfileUpdateRequest.getLastName() != null)
            user.setLastName(userProfileUpdateRequest.getLastName());
        if (userProfileUpdateRequest.getImageUrl() != null)
            user.setImageUrl(userProfileUpdateRequest.getImageUrl());
        if (userProfileUpdateRequest.getPhoneNumber() != null)
            user.setPhoneNumber(userProfileUpdateRequest.getPhoneNumber());
        userRepository.save(user);
        return mapToUserInfo(user);
    }

    public SuccessfulResponse changeOwnPassword(ChangePasswordRequest changePasswordRequest) {
        UserPrincipal userPrincipal = SecurityUtils.getUserPrincipal();
        UserEntity user = userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND,
                        String.format(USER_NOT_FOUND_MSG, userPrincipal.getUserId())));

        if (user.getPassword() != null
                && !passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Wrong password");
        }
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH,
                    "New password and confirm password do not match");
        }

        if (!user.getProvider().contains(AuthProvider.local.name())) {
            user.setProvider(user.getProvider() + "," + AuthProvider.local.name());
        }
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
        return new SuccessfulResponse().result(true);
    }

    private UserInfo mapToUserInfo(UserEntity user) {
       return new UserInfo()
                .userId(user.getUuid())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .imageUrl(user.getImageUrl())
                .provider(user.getProvider())
                .roles(user.getRoles().stream().map(RoleEntity::getName).toList())
                .lastLoginAt((user.getLastLoginAt() != null) ? user.getLastLoginAt().atOffset(ZoneOffset.UTC) : null)
                .createdAt(user.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(user.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
}

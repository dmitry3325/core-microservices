package com.corems.userms.service;

import com.corems.common.security.UserPrincipal;
import com.corems.userms.entity.User;
import com.corems.userms.model.ChangeEmailRequest;
import com.corems.userms.model.ChangePasswordRequest;
import com.corems.userms.model.SuccessfulResponse;
import com.corems.userms.model.UserInfo;
import com.corems.userms.model.UserProfileUpdateRequest;
import com.corems.userms.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.model.exception.AuthServiceException;
import com.corems.userms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserInfo getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        return getUserInfo(userPrincipal.getUserId());
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

        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Wrong password");
        }
        if (changePasswordRequest.getNewPassword() == null || changePasswordRequest.getConfirmPassword() == null ||
            !changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "New password and confirm password do not match");
        }
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse changeOwnEmail(ChangeEmailRequest changeEmailRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userPrincipal.getUserId())));
        // start verification flow - for now just change email
        user.setEmail(changeEmailRequest.getNewEmail());
        userRepository.save(user);
        return new SuccessfulResponse().result(true);
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

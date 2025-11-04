package com.corems.userms.service;

import com.corems.userms.entity.User;
import com.corems.userms.entity.Role;
import com.corems.userms.exception.UserServiceException;
import com.corems.userms.exception.UserServiceExceptionReasonCodes;
import com.corems.userms.model.AdminSetPasswordRequest;
import com.corems.userms.model.ChangeEmailRequest;
import com.corems.userms.model.CreateUserRequest;
import com.corems.userms.model.SuccessfulResponse;
import com.corems.userms.model.UserInfo;
import com.corems.userms.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.model.exception.AuthServiceException;
import com.corems.userms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserInfo getUserById(String userId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

        return new UserInfo()
                .userId(user.getUuid())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl());
    }

    public SuccessfulResponse updateUserById(String userId, UserInfo userInfo) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

        if (userInfo.getFirstName() != null) user.setFirstName(userInfo.getFirstName());
        if (userInfo.getLastName() != null) user.setLastName(userInfo.getLastName());
        if (userInfo.getEmail() != null) user.setEmail(userInfo.getEmail());
        if (userInfo.getImageUrl() != null) user.setImageUrl(userInfo.getImageUrl());

        userRepository.save(user);

        return new SuccessfulResponse().result(true);
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

    public SuccessfulResponse createUser(CreateUserRequest createUserRequest) {
        if (userRepository.findByEmail(createUserRequest.getEmail()).isPresent()) {
            throw UserServiceException.of(UserServiceExceptionReasonCodes.USER_EXISTS, "User with this email already exists");
        }

        User.UserBuilder userBuilder = User.builder()
                .email(createUserRequest.getEmail())
                .firstName(createUserRequest.getFirstName())
                .lastName(createUserRequest.getLastName())
                .provider(com.corems.userms.model.enums.AuthProvider.email)
                .password(passwordEncoder.encode("temporary"));

        User user = userBuilder.build();
        user.setRoles(List.of(new Role(com.corems.userms.model.enums.AppRoles.USER_MS_USER, user)));

        userRepository.save(user);

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse deleteUserById(String userId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

        userRepository.delete(user);
        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse triggerUserResetPassword(String userId) {
        // TODO for now simply verify user exists and return success (real implementation would send email)
        userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse adminChangeUserPassword(String userId, AdminSetPasswordRequest adminSetPasswordRequest) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

        if (adminSetPasswordRequest.getNewPassword() == null || adminSetPasswordRequest.getConfirmPassword() == null ||
            !adminSetPasswordRequest.getNewPassword().equals(adminSetPasswordRequest.getConfirmPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "New password and confirm password do not match");
        }
        user.setPassword(passwordEncoder.encode(adminSetPasswordRequest.getNewPassword()));
        userRepository.save(user);

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse adminChangeUserEmail(String userId, ChangeEmailRequest changeEmailRequest) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

        user.setEmail(changeEmailRequest.getNewEmail());
        userRepository.save(user);

        return new SuccessfulResponse().result(true);
    }
}

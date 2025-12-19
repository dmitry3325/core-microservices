package com.corems.userms.app.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.SecurityUtils;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.entity.RoleEntity;
import com.corems.userms.app.exception.UserServiceExceptionReasonCodes;
import com.corems.userms.api.model.AdminSetPasswordRequest;
import com.corems.userms.api.model.ChangeEmailRequest;
import com.corems.userms.api.model.CreateUserRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.api.model.UserInfo;
import com.corems.userms.api.model.UsersPagedResponse;
import com.corems.userms.app.model.enums.AuthProvider;
import com.corems.userms.app.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.app.model.exception.AuthServiceException;
import com.corems.userms.app.repository.UserRepository;
import com.corems.common.utils.db.utils.QueryParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private static final String USER_NOT_FOUND_MSG = "User id: %s not found";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public UserInfo getUserById(UUID userId) {
        UserEntity user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format(USER_NOT_FOUND_MSG, userId)));

        return mapToUserInfo(user);
    }

    public SuccessfulResponse updateUserById(UUID userId, UserInfo userInfo) {
        UserEntity user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format(USER_NOT_FOUND_MSG, userId)));

        if (userInfo.getFirstName() != null) user.setFirstName(userInfo.getFirstName());
        if (userInfo.getLastName() != null) user.setLastName(userInfo.getLastName());
        if (userInfo.getEmail() != null) user.setEmail(userInfo.getEmail());
        if (userInfo.getImageUrl() != null) user.setImageUrl(userInfo.getImageUrl());
        if (userInfo.getPhoneNumber() != null) user.setPhoneNumber(userInfo.getPhoneNumber());

        if (userInfo.getRoles() != null) {
            assignRoles(user, userInfo.getRoles());
        }

        userRepository.save(user);

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse createUser(CreateUserRequest createUserRequest) {
        if (userRepository.findByEmail(createUserRequest.getEmail()).isPresent()) {
            throw ServiceException.of(UserServiceExceptionReasonCodes.USER_EXISTS, "User with this email already exists");
        }

        UserEntity.UserEntityBuilder userBuilder = UserEntity.builder()
                .email(createUserRequest.getEmail())
                .firstName(createUserRequest.getFirstName())
                .lastName(createUserRequest.getLastName())
                .provider(AuthProvider.local.name())
                .password("{noop}temporary");

        if (createUserRequest.getPhoneNumber() != null) {
            userBuilder.phoneNumber(createUserRequest.getPhoneNumber());
        }

        UserEntity user = userBuilder.build();
        assignRoles(user, createUserRequest.getRoles());

        userRepository.save(user);

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse deleteUserById(UUID userId) {
        UserEntity user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format(USER_NOT_FOUND_MSG, userId)));

        userRepository.delete(user);
        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse adminChangeUserPassword(UUID userId, AdminSetPasswordRequest adminSetPasswordRequest) {
        UserEntity user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format(USER_NOT_FOUND_MSG, userId)));

        if (!adminSetPasswordRequest.getNewPassword().equals(adminSetPasswordRequest.getConfirmPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "New password and confirm password do not match");
        }
        user.setPassword(passwordEncoder.encode(adminSetPasswordRequest.getNewPassword()));
        userRepository.save(user);

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse adminChangeUserEmail(UUID userId, ChangeEmailRequest changeEmailRequest) {
        UserEntity user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format(USER_NOT_FOUND_MSG, userId)));

        user.setEmail(changeEmailRequest.getNewEmail());
        userRepository.save(user);

        return new SuccessfulResponse().result(true);
    }

    public UsersPagedResponse getAllUsers(Optional<Integer> page,
                                          Optional<Integer> pageSize,
                                          Optional<String> search,
                                          Optional<String> sort,
                                          Optional<List<String>> filters) {
        if (sort.isEmpty()) {
            sort = Optional.of("createdAt:desc");
        }
        QueryParams params = new QueryParams(page, pageSize, search, sort, filters);
        Page<UserEntity> userPage = userRepository.findAllByQueryParams(params);
        List<UserInfo> items = userPage.getContent().stream()
                .map(this::mapToUserInfo)
                .toList();

        UsersPagedResponse response = new UsersPagedResponse(userPage.getNumber() + 1, userPage.getSize());
        response.setItems(items);
        response.setTotalPages(userPage.getTotalPages());
        response.setTotalElements(userPage.getTotalElements());
        return response;
    }

    private UserInfo mapToUserInfo(UserEntity user) {
        UserInfo userInfo = new UserInfo()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .imageUrl(user.getImageUrl());

        if (SecurityUtils.hasRole(CoreMsRoles.USER_MS_ADMIN)) {
            userInfo
                    .userId(user.getUuid())
                    .provider(user.getProvider())
                    .roles(user.getRoles().stream().map(RoleEntity::getName).toList())
                    .lastLoginAt((user.getLastLoginAt() != null) ? user.getLastLoginAt().atOffset(ZoneOffset.UTC) : null)
                    .createdAt(user.getCreatedAt().atOffset(ZoneOffset.UTC))
                    .updatedAt(user.getUpdatedAt().atOffset(ZoneOffset.UTC));

        }

        return userInfo;
    }

    // delegate to RoleService
    public void assignRoles(UserEntity user, List<String> desiredRoles) {
        roleService.assignRoles(user, desiredRoles);
    }
}

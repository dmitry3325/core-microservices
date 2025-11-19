package com.corems.userms.app.service;

import com.corems.userms.app.entity.User;
import com.corems.userms.app.entity.Role;
import com.corems.common.security.CoreMsRoles;
import com.corems.userms.app.exception.UserServiceException;
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
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserInfo getUserById(UUID userId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

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

    public SuccessfulResponse updateUserById(UUID userId, UserInfo userInfo) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

        if (userInfo.getFirstName() != null) user.setFirstName(userInfo.getFirstName());
        if (userInfo.getLastName() != null) user.setLastName(userInfo.getLastName());
        if (userInfo.getEmail() != null) user.setEmail(userInfo.getEmail());
        if (userInfo.getImageUrl() != null) user.setImageUrl(userInfo.getImageUrl());

        if (userInfo.getRoles() != null) {
            List<String> desired = userInfo.getRoles().stream().map(String::trim).map(String::toUpperCase).toList();
            for (String rn : desired) {
                try {
                    CoreMsRoles.valueOf(rn);
                } catch (IllegalArgumentException ex) {
                    throw UserServiceException.of(UserServiceExceptionReasonCodes.INVALID_ROLE, "Invalid role: " + rn);
                }
            }

            List<String> current = user.getRoles().stream().map(Role::getName).toList();
            for (String rn : desired) {
                if (!current.contains(rn)) {
                    CoreMsRoles roleEnum = CoreMsRoles.valueOf(rn);
                    user.getRoles().add(new Role(roleEnum, user));
                }
            }

            user.getRoles().removeIf(r -> !desired.contains(r.getName()));
        }

        userRepository.save(user);

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse createUser(CreateUserRequest createUserRequest) {
        if (userRepository.findByEmail(createUserRequest.getEmail()).isPresent()) {
            throw UserServiceException.of(UserServiceExceptionReasonCodes.USER_EXISTS, "User with this email already exists");
        }

        User.UserBuilder userBuilder = User.builder()
                .email(createUserRequest.getEmail())
                .firstName(createUserRequest.getFirstName())
                .lastName(createUserRequest.getLastName())
                .provider(AuthProvider.local.name())
                .password("{noop}temporary");

        User user = userBuilder.build();
        user.setRoles(List.of(new Role(CoreMsRoles.USER_MS_USER, user)));

        userRepository.save(user);

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse deleteUserById(UUID userId) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

        userRepository.delete(user);
        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse triggerUserResetPassword(UUID userId) {
        // TODO for now simply verify user exists and return success (real implementation would send email)
        userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse adminChangeUserPassword(UUID userId, AdminSetPasswordRequest adminSetPasswordRequest) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

        if (adminSetPasswordRequest.getNewPassword() == null || adminSetPasswordRequest.getConfirmPassword() == null ||
            !adminSetPasswordRequest.getNewPassword().equals(adminSetPasswordRequest.getConfirmPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "New password and confirm password do not match");
        }
        user.setPassword("{noop}" + adminSetPasswordRequest.getNewPassword());
        userRepository.save(user);

        return new SuccessfulResponse().result(true);
    }

    public SuccessfulResponse adminChangeUserEmail(UUID userId, ChangeEmailRequest changeEmailRequest) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, String.format("User id: %s not found", userId)));

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
        Page<User> userPage = userRepository.findAllByQueryParams(params);
        List<UserInfo> items = userPage.getContent().stream()
                .map(user -> new UserInfo()
                        .userId(user.getUuid())
                        .provider(user.getProvider())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .imageUrl(user.getImageUrl())
                        .lastLoginAt(user.getLastLogin().atOffset(ZoneOffset.UTC))
                        .createdAt(user.getCreatedAt().atOffset(ZoneOffset.UTC))
                        .updatedAt(user.getUpdatedAt().atOffset(ZoneOffset.UTC)))
                .collect(Collectors.toList());

        UsersPagedResponse response = new UsersPagedResponse(userPage.getNumber() + 1, userPage.getSize());
        response.setItems(items);
        response.setTotalPages(userPage.getTotalPages());
        response.setTotalElements(userPage.getTotalElements());
        return response;
    }
}

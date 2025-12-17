package com.corems.userms.app.controller;

import com.corems.userms.api.UserApi;
import com.corems.userms.api.model.AdminSetPasswordRequest;
import com.corems.userms.api.model.ChangeEmailRequest;
import com.corems.userms.api.model.CreateUserRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.api.model.UserInfo;
import com.corems.userms.api.model.UsersPagedResponse;
import com.corems.userms.app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import com.corems.common.security.RequireRoles;
import com.corems.common.security.CoreMsRoles;

@Log4j2
@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<UserInfo> getUserById(UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @Override
    @RequireRoles(CoreMsRoles.USER_MS_ADMIN)
    public ResponseEntity<SuccessfulResponse> updateUserById(UUID userId, UserInfo userInfo) {
        return ResponseEntity.ok(userService.updateUserById(userId, userInfo));
    }

    @Override
    public ResponseEntity<UsersPagedResponse> getAllUsers(
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> sort,
            Optional<String> search,
            Optional<List<String>> filter) {
        return ResponseEntity.ok(userService.getAllUsers(page, pageSize, search, sort, filter));
    }

    @Override
    @RequireRoles(CoreMsRoles.USER_MS_ADMIN)
    public ResponseEntity<SuccessfulResponse> createUser(CreateUserRequest createUserRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(createUserRequest));
    }

    @Override
    @RequireRoles(CoreMsRoles.USER_MS_ADMIN)
    public ResponseEntity<SuccessfulResponse> deleteUserById(UUID userId) {
        return ResponseEntity.ok(userService.deleteUserById(userId));
    }

    @Override
    @RequireRoles(CoreMsRoles.USER_MS_ADMIN)
    public ResponseEntity<SuccessfulResponse> adminChangeUserPassword(UUID userId, AdminSetPasswordRequest adminSetPasswordRequest) {
        return ResponseEntity.ok(userService.adminChangeUserPassword(userId, adminSetPasswordRequest));
    }

    @Override
    @RequireRoles(CoreMsRoles.USER_MS_ADMIN)
    public ResponseEntity<SuccessfulResponse> adminChangeUserEmail(UUID userId, ChangeEmailRequest changeEmailRequest) {
        return ResponseEntity.ok(userService.adminChangeUserEmail(userId, changeEmailRequest));
    }
}
package com.corems.userms.controller;

import com.corems.userms.api.UserApi;
import com.corems.userms.model.AdminSetPasswordRequest;
import com.corems.userms.model.ChangeEmailRequest;
import com.corems.userms.model.CreateUserRequest;
import com.corems.userms.model.SuccessfulResponse;
import com.corems.userms.model.UserInfo;
import com.corems.userms.model.UsersPagedResponse;
import com.corems.userms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.List;
import com.corems.common.utils.db.spec.FilterRequest;
import com.corems.common.utils.db.spec.FilterOperation;

@Log4j2
@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<UserInfo> getUserById(String userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> updateUserById(String userId, UserInfo userInfo) {
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
    public ResponseEntity<SuccessfulResponse> createUser(CreateUserRequest createUserRequest) {
        return ResponseEntity.ok(userService.createUser(createUserRequest));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> deleteUserById(String userId) {
        return ResponseEntity.ok(userService.deleteUserById(userId));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> triggerUserResetPassword(String userId) {
        return ResponseEntity.ok(userService.triggerUserResetPassword(userId));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> adminChangeUserPassword(String userId, AdminSetPasswordRequest adminSetPasswordRequest) {
        return ResponseEntity.ok(userService.adminChangeUserPassword(userId, adminSetPasswordRequest));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> adminChangeUserEmail(String userId, ChangeEmailRequest changeEmailRequest) {
        return ResponseEntity.ok(userService.adminChangeUserEmail(userId, changeEmailRequest));
    }
}
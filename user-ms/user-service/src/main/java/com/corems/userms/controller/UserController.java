package com.corems.userms.controller;


import com.corems.userms.api.UserApi;
import com.corems.userms.model.SuccessfulResponse;
import com.corems.userms.model.UserInfo;
import com.corems.userms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        return ResponseEntity.ok(userService.updateUserById(userId));
    }

    @Override
    public ResponseEntity<List<UserInfo>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    @Override
    public ResponseEntity<UserInfo> currentUserInfo() {
        return ResponseEntity.ok(userService.getCurrentUserInfo());
    }
}
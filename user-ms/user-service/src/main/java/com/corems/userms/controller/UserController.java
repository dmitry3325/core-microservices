package com.corems.userms.controller;


import com.corems.userms.api.UserApi;
import com.corems.userms.model.UserInfo;
import com.corems.userms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {
    private final UserService userService;

    @Override
    public ResponseEntity<UserInfo> currentUserInfo() {
        return ResponseEntity.ok(userService.getCurrentUserInfo());
    }
}
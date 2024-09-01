package com.corems.authms.controller;


import com.corems.authms.api.UserApi;
import com.corems.authms.model.UserInfo;
import com.corems.authms.service.UserService;
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
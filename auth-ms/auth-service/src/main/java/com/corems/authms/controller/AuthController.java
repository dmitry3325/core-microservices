package com.corems.authms.controller;

import com.corems.authms.api.AuthenticationApi;
import com.corems.authms.model.AccessTokenResponse;
import com.corems.authms.model.LoginRequest;
import com.corems.authms.model.RegisterNewUserRequest;
import com.corems.authms.model.SuccessfulResponse;
import com.corems.authms.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {

    @Autowired
    private final AuthService authService;

    @Override
    public ResponseEntity<AccessTokenResponse> login(LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @Override
    public ResponseEntity<AccessTokenResponse> refreshToken() {
        return ResponseEntity.ok(authService.refreshToken());
    }

    @Override
    public ResponseEntity<SuccessfulResponse> registerUser(RegisterNewUserRequest registerNewUserRequest) {
        return ResponseEntity.ok(authService.registerNewUser(registerNewUserRequest));
    }

}
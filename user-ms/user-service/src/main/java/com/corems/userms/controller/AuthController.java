package com.corems.userms.controller;

import com.corems.userms.api.AuthenticationApi;
import com.corems.userms.model.AccessTokenResponse;
import com.corems.userms.model.LoginRequest;
import com.corems.userms.model.RegisterNewUserRequest;
import com.corems.userms.model.SuccessfulResponse;
import com.corems.userms.model.TokenResponse;
import com.corems.userms.service.AuthService;
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
    public ResponseEntity<TokenResponse> login(LoginRequest loginRequest) {
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
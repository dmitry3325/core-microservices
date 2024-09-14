package com.corems.userms.controller;

import com.corems.userms.api.AuthenticationApi;
import com.corems.userms.model.AccessTokenResponse;
import com.corems.userms.model.SignInRequest;
import com.corems.userms.model.SignUpRequest;
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
    public ResponseEntity<TokenResponse> signIn(SignInRequest loginRequest) {
        return ResponseEntity.ok(authService.signIn(loginRequest));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> signUp(SignUpRequest signUpRequest) {
        return ResponseEntity.ok(authService.signUp(signUpRequest));
    }

    @Override
    public ResponseEntity<AccessTokenResponse> refreshToken() {
        return ResponseEntity.ok(authService.refreshToken());
    }

}
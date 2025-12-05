package com.corems.userms.app.controller;

import com.corems.userms.api.AuthenticationApi;
import com.corems.userms.api.model.AccessTokenResponse;
import com.corems.userms.api.model.SignInRequest;
import com.corems.userms.api.model.SignUpRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.api.model.TokenResponse;
import com.corems.userms.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<SuccessfulResponse> signOut() {
        return ResponseEntity.ok(authService.signOut());
    }

    @Override
    public ResponseEntity<SuccessfulResponse> signUp(SignUpRequest signUpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(signUpRequest));
    }

    @Override
    public ResponseEntity<AccessTokenResponse> refreshToken() {
        return ResponseEntity.ok(authService.getAccessToken());
    }

}
package com.corems.authms.service;

import com.corems.authms.entity.User;
import com.corems.authms.model.AccessTokenResponse;
import com.corems.authms.model.LoginRequest;
import com.corems.authms.model.RegisterNewUserRequest;
import com.corems.authms.model.SuccessfulResponse;
import com.corems.authms.model.enums.AuthProvider;
import com.corems.authms.model.enums.Role;
import com.corems.authms.model.exception.AuthExceptionReasonCodes;
import com.corems.authms.model.exception.AuthServiceException;

import com.corems.authms.repository.UserRepository;
import com.corems.authms.security.TokenProvider;
import com.corems.authms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public AccessTokenResponse login(LoginRequest loginRequest) {
        Authentication authentication;

        User user = userRepository
                .findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, "Email not registered yet."));

        if (user.getProvider() != AuthProvider.email) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PROVIDER_MISMATCH, "Please use same authentication method or register a new account with email and password.");
        }

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, "Invalid email or password.");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.createToken(user.getId(), Map.of(
                TokenProvider.CLAIM_EMAIL, user.getEmail(),
                TokenProvider.CLAIM_USER_NAME, user.getUserName(),
                TokenProvider.CLAIM_ROLES, List.of(Role.USER)
        ));

        return new AccessTokenResponse().accessToken(token);
    }

    public AccessTokenResponse refreshToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        String token = tokenProvider.createToken(userPrincipal.getId(), Map.of(
                TokenProvider.CLAIM_EMAIL, userPrincipal.getEmail(),
                TokenProvider.CLAIM_USER_NAME, userPrincipal.getUsername(),
                TokenProvider.CLAIM_ROLES, List.of(Role.USER)
        ));

        return new AccessTokenResponse().accessToken(token);
    }

    public SuccessfulResponse registerNewUser(RegisterNewUserRequest newUserRequest) {
        if (!Objects.equals(newUserRequest.getPassword(), newUserRequest.getConfirmPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Sorry, confirm password value is not valid.");
        }

        if (userRepository.findByEmail(newUserRequest.getEmail()).isPresent()) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_EXISTS, "User found in the system");
        }

        User.UserBuilder userBuilder = User.builder()
                .email(newUserRequest.getEmail())
                .firstName(newUserRequest.getFirstName())
                .lastName(newUserRequest.getLastName())
                .provider(AuthProvider.email)
                .password(passwordEncoder.encode(newUserRequest.getPassword()));


        if (newUserRequest.getImageUrl() != null) {
            userBuilder.imageUrl(newUserRequest.getImageUrl().toString());
        }

        System.out.println(userBuilder.build());
        var savedUser = userRepository.save(userBuilder.build());

        // TODO send email etc

        return new SuccessfulResponse().result(true);
    }
}

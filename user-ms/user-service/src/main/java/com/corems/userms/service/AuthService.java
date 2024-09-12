package com.corems.userms.service;

import com.corems.userms.entity.User;
import com.corems.userms.model.AccessTokenResponse;
import com.corems.userms.model.LoginRequest;
import com.corems.userms.model.RegisterNewUserRequest;
import com.corems.userms.model.SuccessfulResponse;
import com.corems.userms.model.TokenResponse;
import com.corems.userms.model.enums.AuthProvider;
import com.corems.userms.model.enums.Role;
import com.corems.userms.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.model.exception.AuthServiceException;

import com.corems.userms.repository.UserRepository;
import com.corems.userms.security.TokenProvider;
import com.corems.userms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public TokenResponse login(LoginRequest loginRequest) {
        User user = userRepository
                .findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, "Email not registered yet."));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw  new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Password is incorrect.");
        }

        if (user.getProvider() != AuthProvider.email) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PROVIDER_MISMATCH, "Please use same authentication method or register a new account with email and password.");
        }

        var tokenPayload = Map.of(
                TokenProvider.CLAIM_EMAIL, user.getEmail(),
                TokenProvider.CLAIM_USER_NAME, user.getUserName(),
                TokenProvider.CLAIM_ROLES, List.of(Role.USER)
        );

        String refreshToken = tokenProvider.createRefreshToken(user.getId(), tokenPayload);
        String accessToken = tokenProvider.createAccessToken(user.getId(), tokenPayload);

        return new TokenResponse().refreshToken(refreshToken).accessToken(accessToken);
    }

    public AccessTokenResponse refreshToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        String token = tokenProvider.createAccessToken(userPrincipal.getId(), Map.of(
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

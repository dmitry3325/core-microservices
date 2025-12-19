package com.corems.userms.app.service;

import com.corems.common.security.UserPrincipal;
import com.corems.common.security.service.TokenProvider;

import com.corems.userms.app.entity.LoginTokenEntity;
import com.corems.userms.app.entity.RoleEntity;
import com.corems.userms.app.entity.UserEntity;
import com.corems.common.exception.ServiceException;
import com.corems.common.security.CoreMsRoles;
import com.corems.userms.app.exception.UserServiceExceptionReasonCodes;
import com.corems.userms.api.model.AccessTokenResponse;
import com.corems.userms.api.model.SignInRequest;
import com.corems.userms.api.model.SignUpRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.api.model.TokenResponse;
import com.corems.userms.app.model.enums.AuthProvider;
import com.corems.userms.app.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.app.model.exception.AuthServiceException;

import com.corems.userms.app.repository.LoginTokenRepository;
import com.corems.userms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import com.corems.common.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final LoginTokenRepository loginTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final NotificationService notificationService;

    public TokenResponse signIn(SignInRequest signRequest) {
        UserEntity user = userRepository
                .findByEmail(signRequest.getEmail())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Wrong username or password."));

        if (!passwordEncoder.matches(signRequest.getPassword(), user.getPassword())) {
            throw  new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Wrong username or password.");
        }

        if (!user.getProvider().contains(AuthProvider.local.name())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PROVIDER_MISMATCH, "Please use same authentication method or register a new account with email and password.");
        }

        String refreshToken = createRefreshToken(user);
        String accessToken = tokenProvider.createAccessToken(UUID.randomUUID().toString(), getClaims(user));

        return new TokenResponse()
                .refreshToken(refreshToken)
                .accessToken(accessToken);
    }

    @Transactional
    public SuccessfulResponse signOut() {
        UserPrincipal userPrincipal = SecurityUtils.getUserPrincipal();

        validateRefreshToken(userPrincipal);
        log.info("Deleting token with ID: {}", userPrincipal.getTokenId());
        loginTokenRepository.deleteByUuid(userPrincipal.getTokenId());

        return new SuccessfulResponse().result(true);
    }

    public AccessTokenResponse getAccessToken() {
        UserPrincipal userPrincipal = SecurityUtils.getUserPrincipal();

        validateRefreshToken(userPrincipal);

        String token = tokenProvider.createAccessToken(UUID.randomUUID().toString(), Map.of(
                TokenProvider.CLAIM_USER_ID, userPrincipal.getUserId(),
                TokenProvider.CLAIM_EMAIL, userPrincipal.getEmail(),
                TokenProvider.CLAIM_FIRST_NAME, userPrincipal.getFirstName(),
                TokenProvider.CLAIM_LAST_NAME, userPrincipal.getLastName(),
                TokenProvider.CLAIM_ROLES, userPrincipal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        ));

        return new AccessTokenResponse().accessToken(token);
    }

    public String createRefreshToken(UserEntity user) {
        UUID tokenId = UUID.randomUUID();
        String refreshToken = tokenProvider.createRefreshToken(tokenId.toString(), getClaims(user));

        LoginTokenEntity loginToken = new LoginTokenEntity();
        loginToken.setUuid(tokenId);
        loginToken.setUser(user);
        loginToken.setToken(refreshToken);
        loginTokenRepository.save(loginToken);

        return refreshToken;
    }

    private void validateRefreshToken(UserPrincipal userPrincipal) {
        LoginTokenEntity refreshToken = loginTokenRepository
                .findByUuid(userPrincipal.getTokenId())
                .orElseThrow(() ->  ServiceException.of(UserServiceExceptionReasonCodes.TOKEN_NOT_FOUND, String.format("Token not found with ID: %s.", userPrincipal.getTokenId())));

        if (!Objects.equals(userPrincipal.getUserId(), refreshToken.getUser().getUuid())) {
            throw ServiceException.of(UserServiceExceptionReasonCodes.TOKEN_NOT_FOUND, String.format("Token not found with ID: %s.", userPrincipal.getTokenId()));
        }
    }

    private Map<String, Object> getClaims(UserEntity user) {
        return Map.of(
                TokenProvider.CLAIM_USER_ID, user.getUuid(),
                TokenProvider.CLAIM_EMAIL, user.getEmail(),
                TokenProvider.CLAIM_FIRST_NAME, user.getFirstName(),
                TokenProvider.CLAIM_LAST_NAME, user.getLastName(),
                TokenProvider.CLAIM_ROLES, user.getRoles().stream().map(RoleEntity::getName).toList()
        );
    }

    public SuccessfulResponse signUp(SignUpRequest signUpRequest) {
        log.info("Sign up request for email: {}", signUpRequest.getEmail());
        if (!Objects.equals(signUpRequest.getPassword(), signUpRequest.getConfirmPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Sorry, confirm password value is not valid.");
        }

        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_EXISTS, "User found in the system");
        }

        UserEntity.UserEntityBuilder userBuilder = UserEntity.builder()
                .email(signUpRequest.getEmail())
                .firstName(signUpRequest.getFirstName())
                .lastName(signUpRequest.getLastName())
                .provider(AuthProvider.local.name())
                .password(passwordEncoder.encode(signUpRequest.getPassword()));


        if (signUpRequest.getImageUrl() != null) {
            userBuilder.imageUrl(signUpRequest.getImageUrl());
        }

        UserEntity user = userBuilder.build();
        user.setRoles(List.of(new RoleEntity(CoreMsRoles.USER_MS_USER, user)));

        var savedUser = userRepository.save(user);

        notificationService.sendWelcomeEmail(savedUser);

        return new SuccessfulResponse().result(true);
    }
}

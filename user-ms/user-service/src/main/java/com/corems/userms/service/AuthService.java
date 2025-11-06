package com.corems.userms.service;

import com.corems.common.security.UserPrincipal;
import com.corems.common.security.service.TokenProvider;
import com.corems.userms.entity.LoginToken;
import com.corems.userms.entity.Role;
import com.corems.userms.entity.User;
import com.corems.userms.exception.UserServiceException;
import com.corems.userms.exception.UserServiceExceptionReasonCodes;
import com.corems.userms.model.AccessTokenResponse;
import com.corems.userms.model.SignInRequest;
import com.corems.userms.model.SignUpRequest;
import com.corems.userms.model.SuccessfulResponse;
import com.corems.userms.model.TokenResponse;
import com.corems.userms.model.enums.AuthProvider;
import com.corems.userms.model.enums.AppRoles;
import com.corems.userms.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.model.exception.AuthServiceException;

import com.corems.userms.repository.LoginTokenRepository;
import com.corems.userms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

    public TokenResponse signIn(SignInRequest signRequest) {
        User user = userRepository
                .findByEmail(signRequest.getEmail())
                .orElseThrow(() -> new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Wrong username or password."));

        if (!passwordEncoder.matches(signRequest.getPassword(), user.getPassword())) {
            throw  new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Wrong username or password.");
        }

        if (!user.getProvider().contains(AuthProvider.email.name())) {
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        validateRefreshToken(userPrincipal);
        System.out.println("Deleting token with ID: " + userPrincipal.getTokenId());
        loginTokenRepository.deleteByUuid(userPrincipal.getTokenId());

        return new SuccessfulResponse().result(true);
    }

    public AccessTokenResponse getAccessToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

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

    public String createRefreshToken(User user) {
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = tokenProvider.createRefreshToken(tokenId, getClaims(user));

        LoginToken loginToken = new LoginToken();
        loginToken.setUuid(tokenId);
        loginToken.setUser(user);
        loginToken.setToken(refreshToken);
        loginTokenRepository.save(loginToken);

        return refreshToken;
    }

    private void validateRefreshToken(UserPrincipal userPrincipal) {
        LoginToken refreshToken = loginTokenRepository
                .findByUuid(userPrincipal.getTokenId())
                .orElseThrow(() ->  UserServiceException.of(UserServiceExceptionReasonCodes.TOKEN_NOT_FOUND, String.format("Token not found with ID: %s.", userPrincipal.getTokenId())));

        if (!Objects.equals(userPrincipal.getUserId(), refreshToken.getUser().getUuid())) {
            throw UserServiceException.of(UserServiceExceptionReasonCodes.TOKEN_NOT_FOUND, String.format("Token not found with ID: %s.", userPrincipal.getTokenId()));
        }
    }

    private Map<String, Object> getClaims(User user) {
        return Map.of(
                TokenProvider.CLAIM_USER_ID, user.getUuid(),
                TokenProvider.CLAIM_EMAIL, user.getEmail(),
                TokenProvider.CLAIM_FIRST_NAME, user.getFirstName(),
                TokenProvider.CLAIM_LAST_NAME, user.getLastName(),
                TokenProvider.CLAIM_ROLES, user.getRoles().stream().map(Role::getName).toList()
        );
    }

    public SuccessfulResponse signUp(SignUpRequest signUpRequest) {
        log.error("Sign up request: {}", signUpRequest);
        if (!Objects.equals(signUpRequest.getPassword(), signUpRequest.getConfirmPassword())) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_PASSWORD_MISMATCH, "Sorry, confirm password value is not valid.");
        }

        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_EXISTS, "User found in the system");
        }

        User.UserBuilder userBuilder = User.builder()
                .email(signUpRequest.getEmail())
                .firstName(signUpRequest.getFirstName())
                .lastName(signUpRequest.getLastName())
                .provider(AuthProvider.email.name())
                .password(passwordEncoder.encode(signUpRequest.getPassword()));


        if (signUpRequest.getImageUrl() != null) {
            userBuilder.imageUrl(signUpRequest.getImageUrl().toString());
        }

        User user = userBuilder.build();
        user.setRoles(List.of(new Role(AppRoles.USER_MS_USER, user)));

        var savedUser = userRepository.save(user);

        // TODO send email etc

        return new SuccessfulResponse().result(true);
    }
}

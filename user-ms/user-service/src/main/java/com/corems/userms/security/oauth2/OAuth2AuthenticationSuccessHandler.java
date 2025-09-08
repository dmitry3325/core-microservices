package com.corems.userms.security.oauth2;

import com.corems.common.security.UserPrincipal;
import com.corems.common.security.token.TokenProvider;
import com.corems.common.service.exception.handler.DefaultExceptionReasonCodes;
import com.corems.userms.entity.LoginToken;
import com.corems.userms.exception.UserServiceException;
import com.corems.userms.model.enums.AppRoles;
import com.corems.userms.repository.LoginTokenRepository;
import com.corems.userms.repository.UserRepository;
import com.corems.userms.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final LoginTokenRepository loginTokenRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtils
                .getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String tokenId = UUID.randomUUID().toString();
        String token = tokenProvider.createRefreshToken(tokenId, Map.of(
                TokenProvider.CLAIM_USER_ID, userPrincipal.getUserId(),
                TokenProvider.CLAIM_EMAIL, userPrincipal.getEmail(),
                TokenProvider.CLAIM_FIRST_NAME, userPrincipal.getFirstName(),
                TokenProvider.CLAIM_LAST_NAME, userPrincipal.getLastName(),
                TokenProvider.CLAIM_ROLES, userPrincipal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        ));

        LoginToken loginToken = new LoginToken();
        loginToken.setUuid(tokenId);
        loginToken.setUser(userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> UserServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, String.format("User not found with ID: %s.", userPrincipal.getUserId()))));
        loginToken.setToken(token);

        loginTokenRepository.save(loginToken);

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", token)
                .build().toUriString();
    }


    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}

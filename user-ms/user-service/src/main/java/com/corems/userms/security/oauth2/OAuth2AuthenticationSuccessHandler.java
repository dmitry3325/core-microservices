package com.corems.userms.security.oauth2;

import com.corems.userms.entity.LoginToken;
import com.corems.userms.model.enums.Role;
import com.corems.userms.repository.LoginTokenRepository;
import com.corems.userms.repository.UserRepository;
import com.corems.userms.security.TokenProvider;
import com.corems.userms.security.UserPrincipal;
import com.corems.userms.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        String token = tokenProvider.createRefreshToken(userPrincipal.getUserId(), Map.of(
                TokenProvider.CLAIM_TOKEN_ID, tokenId,
                TokenProvider.CLAIM_EMAIL, userPrincipal.getEmail(),
                TokenProvider.CLAIM_USER_NAME, userPrincipal.getUsername(),
                TokenProvider.CLAIM_ROLES, List.of(Role.USER)
        ));

        LoginToken loginToken = new LoginToken();
        loginToken.setUuid(tokenId);
        loginToken.setUser(userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException(String.format("User not found with ID: %s.", userPrincipal.getUserId()))));
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

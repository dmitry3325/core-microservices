package com.corems.userms.app.security.oauth2;

import com.corems.common.security.UserPrincipal;
import com.corems.common.security.service.TokenProvider;
import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.userms.app.entity.LoginTokenEntity;
import com.corems.userms.app.repository.LoginTokenRepository;
import com.corems.userms.app.repository.UserRepository;
import com.corems.userms.app.util.CookieUtils;
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

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtils
                .getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        UUID tokenId = UUID.randomUUID();
        String token = tokenProvider.createRefreshToken(tokenId.toString(), Map.of(
                TokenProvider.CLAIM_USER_ID, userPrincipal.getUserId(),
                TokenProvider.CLAIM_EMAIL, userPrincipal.getEmail(),
                TokenProvider.CLAIM_FIRST_NAME, userPrincipal.getFirstName(),
                TokenProvider.CLAIM_LAST_NAME, userPrincipal.getLastName(),
                TokenProvider.CLAIM_ROLES, userPrincipal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        ));

        LoginTokenEntity loginToken = new LoginTokenEntity();
        loginToken.setUuid(tokenId);
        loginToken.setUser(userRepository.findByUuid(userPrincipal.getUserId())
                .orElseThrow(() -> ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, String.format("User not found with ID: %s.", userPrincipal.getUserId()))));
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

package com.corems.authms.security.oauth2.user;

import com.corems.authms.controller.model.exception.AuthExceptionReasonCodes;
import com.corems.authms.controller.model.exception.AuthServiceException;
import com.corems.authms.controller.model.enums.AuthProvider;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(AuthProvider.google.toString())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.github.toString())) {
            return new GithubOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.linkedin.toString())) {
            return new LinkedinOAuth2UserInfo(attributes);
        } else {
            throw new AuthServiceException(AuthExceptionReasonCodes.PROVIDER_IS_NOT_SUPPORTED, String.format("Login with %s is not supported.", registrationId));
        }
    }

}

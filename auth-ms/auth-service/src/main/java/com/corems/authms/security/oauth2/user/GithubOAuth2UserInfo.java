package com.corems.authms.security.oauth2.user;

import java.util.Map;

public class GithubOAuth2UserInfo extends OAuth2UserInfo {
    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return ((Integer) attributes.get("id")).toString();
    }

    //TODO fix attributes.get
    @Override
    public String getFullName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getFirstName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getLastName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("avatar_url");
    }
}

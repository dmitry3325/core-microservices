package com.corems.userms.app.security.oauth2.provider;

import java.util.Map;

public class LinkedinOAuth2UserInfo extends OAuth2UserInfo {

    public LinkedinOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
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
        return (String) attributes.get("picture");
    }
}

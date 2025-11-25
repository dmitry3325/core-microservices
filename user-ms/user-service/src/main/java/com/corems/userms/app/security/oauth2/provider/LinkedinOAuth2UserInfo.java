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

    @Override
    public String getFullName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getFirstName() {
        String givenName = (String) attributes.get("given_name");
        if (givenName != null) {
            return givenName;
        }
        String fullName = getFullName();
        if (fullName != null && fullName.contains(" ")) {
            return fullName.split(" ")[0];
        }
        return fullName;
    }

    @Override
    public String getLastName() {
        String familyName = (String) attributes.get("family_name");
        if (familyName != null) {
            return familyName;
        }
        String fullName = getFullName();
        if (fullName != null && fullName.contains(" ")) {
            String[] parts = fullName.split(" ");
            return parts[parts.length - 1];
        }
        return null;
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

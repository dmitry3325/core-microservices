package com.corems.common.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Data
public final class UserPrincipal implements UserDetails, OAuth2User {
    private UUID userId;
    private final String email;
    private final String firstName;
    private final String lastName;
    private String password;
    private UUID tokenId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String userId,
                         String email,
                         String firstName,
                         String lastName,
                         String tokenId,
                         Collection<? extends GrantedAuthority> authorities) {
        this.setTokenId(tokenId);
        this.setUserId(userId);
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.authorities = authorities;
    }

    public UserPrincipal(UUID userId,
                         String email,
                         String firstName,
                         String lastName,
                         UUID tokenId,
                         Collection<? extends GrantedAuthority> authorities) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.authorities = authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return firstName + " " + lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public String getEmail() {
        return this.email;
    }

    public UUID getTokenId() {
        return this.tokenId;
    }

    public void setUserId(String userId) {
        if (userId != null) {
            this.userId = UUID.fromString(userId);
        }
    }

    public void setTokenId(String tokenId) {
        if (tokenId != null) {
            this.tokenId = UUID.fromString(tokenId);
        }
    }
}

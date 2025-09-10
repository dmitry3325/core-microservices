package com.corems.userms.security;

import com.corems.common.security.UserPrincipal;
import com.corems.userms.entity.LoginToken;
import com.corems.userms.entity.User;
import com.corems.userms.exception.UserServiceException;
import com.corems.userms.exception.UserServiceExceptionReasonCodes;
import com.corems.userms.repository.LoginTokenRepository;
import com.corems.userms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginTokenRepository loginTokenRepository;

    @Override
    public UserPrincipal loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> UserServiceException.of(UserServiceExceptionReasonCodes.USER_NOT_FOUND, String.format("User not found with email: %s.", email)));

        return new UserPrincipal(
                user.getUuid(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).toList()
        );
    }

    public UserPrincipal loadUserById(String userId, String tokenId) {
        User user = userRepository
                .findByUuid(userId)
                .orElseThrow(() -> UserServiceException.of(UserServiceExceptionReasonCodes.USER_NOT_FOUND, String.format("User not found with ID: %s.", userId)));

        LoginToken token = loginTokenRepository
                .findByUuid(tokenId)
                .orElseThrow(() ->  UserServiceException.of(UserServiceExceptionReasonCodes.TOKEN_NOT_FOUND, String.format("Token not found with ID: %s.", tokenId)));

        if (!Objects.equals(userId, token.getUser().getUuid())) {
            throw UserServiceException.of(UserServiceExceptionReasonCodes.TOKEN_NOT_FOUND, String.format("Token not found with ID: %s.", tokenId));
        }

        return new UserPrincipal(
                user.getUuid(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).toList()
        );
    }

}

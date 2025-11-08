package com.corems.userms.security;

import com.corems.common.security.UserPrincipal;
import com.corems.userms.entity.User;
import com.corems.userms.exception.UserServiceException;
import com.corems.userms.exception.UserServiceExceptionReasonCodes;
import com.corems.userms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

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
                null,
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).toList()
        );
    }

    public UserPrincipal loadUserById(String userId, String tokenId) {
        User user = userRepository
                .findByUuid(userId)
                .orElseThrow(() -> UserServiceException.of(UserServiceExceptionReasonCodes.USER_NOT_FOUND, String.format("User not found with ID: %s.", userId)));

        return new UserPrincipal(
                user.getUuid(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                tokenId,
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).toList()
        );
    }

}

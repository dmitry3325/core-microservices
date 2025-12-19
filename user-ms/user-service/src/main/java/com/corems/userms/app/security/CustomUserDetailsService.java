package com.corems.userms.app.security;

import com.corems.common.exception.ServiceException;
import com.corems.common.security.UserPrincipal;
import com.corems.userms.app.entity.UserEntity;
import com.corems.userms.app.exception.UserServiceExceptionReasonCodes;
import com.corems.userms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserPrincipal loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> ServiceException.of(UserServiceExceptionReasonCodes.USER_NOT_FOUND, String.format("User not found with email: %s.", email)));

        return new UserPrincipal(
                user.getUuid().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                null,
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).toList()
        );
    }

    public UserPrincipal loadUserById(UUID userId, UUID tokenId) {
        UserEntity user = userRepository
                .findByUuid(userId)
                .orElseThrow(() -> ServiceException.of(UserServiceExceptionReasonCodes.USER_NOT_FOUND, String.format("User not found with ID: %s.", userId)));

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

package com.corems.userms.security.oauth2;

import com.corems.common.security.UserPrincipal;
import com.corems.userms.entity.Role;
import com.corems.userms.entity.User;
import com.corems.userms.model.enums.AppRoles;
import com.corems.userms.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.model.exception.AuthServiceException;
import com.corems.userms.model.enums.AuthProvider;
import com.corems.userms.repository.UserRepository;
import com.corems.userms.security.oauth2.provider.OAuth2UserInfo;
import com.corems.userms.security.oauth2.provider.OAuth2UserInfoFactory;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }

    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                oAuth2UserRequest.getClientRegistration().getRegistrationId(),
                oAuth2User.getAttributes()
        );

        if (StringUtils.isEmpty(oAuth2UserInfo.getEmail())) {
            log.error("Email not found from OAuth2 provider");
            throw new AuthServiceException(AuthExceptionReasonCodes.USER_NOT_FOUND, "Email not found from OAuth2 provider");
        }

        AuthProvider authProvider = AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId());
        log.info("Login from provider: {}. Email: {}", authProvider, oAuth2UserInfo.getEmail());

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if(!user.getProvider().contains(authProvider.name())) {
                user.setProvider(user.getProvider() + "," + authProvider.name());
            }
            user.setLastLogin(Instant.now());
            userRepository.save(user);

            log.info("User found id: {}", user.getId());
        } else {
            User newUser = new User();
            newUser.setEmail(oAuth2UserInfo.getEmail());
            newUser.setFirstName(oAuth2UserInfo.getFirstName());
            newUser.setLastName(oAuth2UserInfo.getLastName());
            newUser.setProvider(authProvider.name());
            newUser.setImageUrl(oAuth2UserInfo.getImageUrl());
            newUser.setRoles(List.of(new Role(AppRoles.USER_MS_USER, newUser)));

            user = userRepository.save(newUser);

            log.info("User not exists, created new: {}", user.getId());

        }

        return new UserPrincipal(
                user.getUuid(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                null,
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).toList()
        );
    }
}

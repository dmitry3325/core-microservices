package com.corems.common.security;

import com.corems.common.exception.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityUtilsTest {

    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserPrincipalOptional_WhenNoAuthentication_ReturnsEmpty() {
        when(securityContext.getAuthentication()).thenReturn(null);

        Optional<UserPrincipal> result = SecurityUtils.getUserPrincipalOptional();

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserPrincipalOptional_WhenAuthenticationWithUserPrincipal_ReturnsUserPrincipal() {
        UserPrincipal userPrincipal = createTestUserPrincipal(CoreMsRoles.USER_MS_USER);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        Optional<UserPrincipal> result = SecurityUtils.getUserPrincipalOptional();

        assertTrue(result.isPresent());
        assertEquals(userPrincipal, result.get());
    }

    @Test
    void getUserPrincipalOptional_WhenAuthenticationWithNonUserPrincipal_ReturnsEmpty() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("some-string-principal");

        Optional<UserPrincipal> result = SecurityUtils.getUserPrincipalOptional();

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserPrincipal_WhenUserPrincipalExists_ReturnsUserPrincipal() {
        UserPrincipal userPrincipal = createTestUserPrincipal(CoreMsRoles.USER_MS_USER);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        UserPrincipal result = SecurityUtils.getUserPrincipal();

        assertEquals(userPrincipal, result);
    }

    @Test
    void getUserPrincipal_WhenNoUserPrincipal_ThrowsAuthServiceException() {
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(ServiceException.class, SecurityUtils::getUserPrincipal);
    }

    @Test
    void hasRole_WhenUserHasSpecificRole_ReturnsTrue() {
        UserPrincipal userPrincipal = createTestUserPrincipal(CoreMsRoles.USER_MS_ADMIN);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        boolean result = SecurityUtils.hasRole(CoreMsRoles.USER_MS_ADMIN);

        assertTrue(result);
    }

    @Test
    void hasRole_WhenUserHasSystemRole_ReturnsTrue() {
        UserPrincipal userPrincipal = createTestUserPrincipal(CoreMsRoles.SYSTEM);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        boolean result = SecurityUtils.hasRole(CoreMsRoles.USER_MS_USER);

        assertTrue(result);
    }

    @Test
    void hasRole_WhenUserHasSuperAdminRole_ReturnsTrue() {
        UserPrincipal userPrincipal = createTestUserPrincipal(CoreMsRoles.SUPER_ADMIN);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        boolean result = SecurityUtils.hasRole(CoreMsRoles.DOCUMENT_MS_USER);

        assertTrue(result);
    }

    @Test
    void hasRole_WhenUserDoesNotHaveRole_ReturnsFalse() {
        UserPrincipal userPrincipal = createTestUserPrincipal(CoreMsRoles.USER_MS_USER);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        boolean result = SecurityUtils.hasRole(CoreMsRoles.DOCUMENT_MS_ADMIN);

        assertFalse(result);
    }

    @Test
    void hasRole_WhenNoAuthentication_ReturnsFalse() {
        when(securityContext.getAuthentication()).thenReturn(null);

        boolean result = SecurityUtils.hasRole(CoreMsRoles.USER_MS_USER);

        assertFalse(result);
    }

    @Test
    void hasRole_WhenUserHasMultipleRoles_ChecksCorrectly() {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(CoreMsRoles.USER_MS_USER.name()),
            new SimpleGrantedAuthority(CoreMsRoles.COMMUNICATION_MS_USER.name())
        );
        UserPrincipal userPrincipal = new UserPrincipal(
            UUID.randomUUID(),
            "test@example.com",
            "John",
            "Doe",
            UUID.randomUUID(),
            authorities
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        assertTrue(SecurityUtils.hasRole(CoreMsRoles.USER_MS_USER));
        assertTrue(SecurityUtils.hasRole(CoreMsRoles.COMMUNICATION_MS_USER));
        assertFalse(SecurityUtils.hasRole(CoreMsRoles.DOCUMENT_MS_ADMIN));
    }

    private UserPrincipal createTestUserPrincipal(CoreMsRoles role) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role.name()));
        return new UserPrincipal(
            UUID.randomUUID(),
            "test@example.com",
            "John",
            "Doe",
            UUID.randomUUID(),
            authorities
        );
    }
}
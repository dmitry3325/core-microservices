package com.corems.userms.app.service;

import com.corems.common.security.CoreMsRoles;
import com.corems.userms.app.config.UserServiceProperties;
import com.corems.userms.app.entity.RoleEntity;
import com.corems.userms.app.entity.UserEntity;
import com.corems.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private UserServiceProperties userServiceProperties;

    @InjectMocks
    private RoleService roleService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
        testUser.setRoles(new ArrayList<>());
    }

    @Test
    void assignRoles_WhenValidRoles_ShouldAssignRoles() {
        // Given
        List<String> desiredRoles = List.of("USER_MS_USER", "USER_MS_ADMIN");

        // When
        roleService.assignRoles(testUser, desiredRoles);

        // Then
        assertThat(testUser.getRoles()).hasSize(2);
        List<String> roleNames = testUser.getRoles().stream()
                .map(RoleEntity::getName)
                .toList();
        assertThat(roleNames).containsExactlyInAnyOrder("USER_MS_USER", "USER_MS_ADMIN");
    }

    @Test
    void assignRoles_WhenNullRoles_ShouldUseDefaultRoles() {
        // Given
        when(userServiceProperties.getDefaultRoles()).thenReturn(List.of("USER_MS_USER"));

        // When
        roleService.assignRoles(testUser, null);

        // Then
        assertThat(testUser.getRoles()).hasSize(1);
        assertThat(testUser.getRoles().iterator().next().getName()).isEqualTo("USER_MS_USER");
        verify(userServiceProperties).getDefaultRoles();
    }

    @Test
    void assignRoles_WhenNoDefaultRoles_ShouldUseUserRole() {
        // Given
        when(userServiceProperties.getDefaultRoles()).thenReturn(null);

        // When & Then - This should throw an exception because "USER" is not a valid role
        assertThatThrownBy(() -> roleService.assignRoles(testUser, null))
                .isInstanceOf(ServiceException.class)
                .satisfies(throwable -> {
                    ServiceException exception = (ServiceException) throwable;
                    assertThat(exception.getErrors()).isNotEmpty();
                    assertThat(exception.getErrors().get(0).getDetails()).contains("Invalid role: USER");
                });
    }

    @Test
    void assignRoles_WhenInvalidRole_ShouldThrowException() {
        // Given
        List<String> invalidRoles = List.of("INVALID_ROLE");

        // When & Then
        assertThatThrownBy(() -> roleService.assignRoles(testUser, invalidRoles))
                .isInstanceOf(ServiceException.class)
                .satisfies(throwable -> {
                    ServiceException exception = (ServiceException) throwable;
                    assertThat(exception.getErrors()).isNotEmpty();
                    assertThat(exception.getErrors().get(0).getDetails()).contains("Invalid role: INVALID_ROLE");
                });
    }

    @Test
    void assignRoles_WhenMixedCaseRoles_ShouldNormalizeToUpperCase() {
        // Given
        List<String> mixedCaseRoles = List.of("user_ms_user", "User_Ms_Admin");

        // When
        roleService.assignRoles(testUser, mixedCaseRoles);

        // Then
        assertThat(testUser.getRoles()).hasSize(2);
        List<String> roleNames = testUser.getRoles().stream()
                .map(RoleEntity::getName)
                .toList();
        assertThat(roleNames).containsExactlyInAnyOrder("USER_MS_USER", "USER_MS_ADMIN");
    }

    @Test
    void assignRoles_WhenUserHasExistingRoles_ShouldClearAndAssignNew() {
        // Given
        testUser.getRoles().add(new RoleEntity(CoreMsRoles.SYSTEM, testUser));
        List<String> newRoles = List.of("USER_MS_USER");

        // When
        roleService.assignRoles(testUser, newRoles);

        // Then
        assertThat(testUser.getRoles()).hasSize(1);
        assertThat(testUser.getRoles().iterator().next().getName()).isEqualTo("USER_MS_USER");
    }
}
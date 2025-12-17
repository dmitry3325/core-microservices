package com.corems.userms.app.service;

import com.corems.common.security.CoreMsRoles;
import com.corems.userms.app.entity.RoleEntity;
import com.corems.userms.app.entity.UserEntity;
import com.corems.common.exception.ServiceException;
import com.corems.userms.app.repository.UserRepository;
import com.corems.userms.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = UserEntity.builder()
                .uuid(testUserId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .provider("local")
                .password("encoded_password")
                .phoneNumber("+1234567890")
                .imageUrl("http://example.com/image.jpg")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        // Initialize roles collection
        testUser.setRoles(new ArrayList<>());
        testUser.getRoles().add(new RoleEntity(CoreMsRoles.USER_MS_USER, testUser));
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUserInfo() {
        // Given
        when(userRepository.findByUuid(testUserId)).thenReturn(Optional.of(testUser));

        // When
        UserInfo result = userService.getUserById(testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
    }

    @Test
    void getUserById_WhenUserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByUuid(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(testUserId))
                .isInstanceOf(ServiceException.class)
                .satisfies(throwable -> {
                    ServiceException exception = (ServiceException) throwable;
                    assertThat(exception.getErrors()).isNotEmpty();
                    assertThat(exception.getErrors().get(0).getDetails()).contains("User id: " + testUserId + " not found");
                });
    }

    @Test
    void createUser_WhenValidRequest_ShouldCreateUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest()
                .email("new@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("+9876543210")
                .roles(List.of("USER_MS_USER"));

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // When
        SuccessfulResponse result = userService.createUser(request);

        // Then
        assertThat(result.getResult()).isTrue();
        verify(userRepository).save(any(UserEntity.class));
        verify(roleService).assignRoles(any(UserEntity.class), eq(request.getRoles()));
    }

    @Test
    void deleteUserById_WhenUserExists_ShouldDeleteUser() {
        // Given
        when(userRepository.findByUuid(testUserId)).thenReturn(Optional.of(testUser));

        // When
        SuccessfulResponse result = userService.deleteUserById(testUserId);

        // Then
        assertThat(result.getResult()).isTrue();
        verify(userRepository).delete(testUser);
    }

    @Test
    void adminChangeUserPassword_WhenValidRequest_ShouldChangePassword() {
        // Given
        AdminSetPasswordRequest request = new AdminSetPasswordRequest()
                .newPassword("newPassword123")
                .confirmPassword("newPassword123");

        when(userRepository.findByUuid(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded_new_password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // When
        SuccessfulResponse result = userService.adminChangeUserPassword(testUserId, request);

        // Then
        assertThat(result.getResult()).isTrue();
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(testUser);
        assertThat(testUser.getPassword()).isEqualTo("encoded_new_password");
    }

    @Test
    void adminChangeUserPassword_WhenPasswordMismatch_ShouldThrowException() {
        // Given
        AdminSetPasswordRequest request = new AdminSetPasswordRequest()
                .newPassword("newPassword123")
                .confirmPassword("differentPassword");

        when(userRepository.findByUuid(testUserId)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.adminChangeUserPassword(testUserId, request))
                .isInstanceOf(ServiceException.class)
                .satisfies(throwable -> {
                    ServiceException exception = (ServiceException) throwable;
                    assertThat(exception.getErrors()).isNotEmpty();
                    assertThat(exception.getErrors().get(0).getDetails()).contains("New password and confirm password do not match");
                });
    }


    @Test
    void adminChangeUserEmail_WhenValidRequest_ShouldChangeEmail() {
        // Given
        ChangeEmailRequest request = new ChangeEmailRequest()
                .newEmail("newemail@example.com");

        when(userRepository.findByUuid(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // When
        SuccessfulResponse result = userService.adminChangeUserEmail(testUserId, request);

        // Then
        assertThat(result.getResult()).isTrue();
        verify(userRepository).save(testUser);
        assertThat(testUser.getEmail()).isEqualTo("newemail@example.com");
    }
}
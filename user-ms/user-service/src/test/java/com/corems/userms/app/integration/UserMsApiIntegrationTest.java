package com.corems.userms.app.integration;

import com.corems.userms.ApiClient;
import com.corems.userms.api.model.*;
import com.corems.userms.client.AuthenticationApi;
import com.corems.userms.client.ProfileApi;
import com.corems.userms.client.UserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserMsApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApiClient apiClient;
    @Autowired
    private AuthenticationApi authenticationApi;
    @Autowired
    private ProfileApi profileApi;
    @Autowired
    private UserApi userApi;

    private SignUpRequest signUpRequest;
    private SignInRequest signInRequest;

    @BeforeEach
    void setUp() {
        apiClient.setBasePath("http://localhost:" + port);
        
        // Prepare test data with unique email for each test run
        String uniqueEmail = "testuser" + System.currentTimeMillis() + "@example.com";
        signUpRequest = new SignUpRequest();
        signUpRequest.setEmail(uniqueEmail);
        signUpRequest.setPassword("TestPassword123!");
        signUpRequest.setConfirmPassword("TestPassword123!");
        signUpRequest.setFirstName("Test");
        signUpRequest.setLastName("User");

        signInRequest = new SignInRequest();
        signInRequest.setEmail(uniqueEmail);
        signInRequest.setPassword("TestPassword123!");
    }

    /**
     * Helper method to create a user and authenticate, setting the bearer token on the ApiClient.
     * @return TokenResponse containing access and refresh tokens
     */
    private TokenResponse createUserAndAuthenticate() {
        authenticationApi.signUp(signUpRequest);
        TokenResponse tokenResponse = authenticationApi.signIn(signInRequest);
        apiClient.setBearerToken(tokenResponse.getAccessToken());
        return tokenResponse;
    }

    // ==================== Public Endpoints (No Auth Required) ====================

    @Test
    @Order(1)
    @DirtiesContext
    void signUp_WhenValidRequest_ShouldCreateUser() {
        // When
        SuccessfulResponse response = authenticationApi.signUp(signUpRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isTrue();
    }

    @Test
    @Order(2)
    void signIn_WhenValidCredentials_ShouldReturnTokens() {
        // Given - create user first
        authenticationApi.signUp(signUpRequest);

        // When
        TokenResponse response = authenticationApi.signIn(signInRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
    }

    @Test
    @Order(3)
    void signIn_WhenInvalidCredentials_ShouldThrowException() {
        // Given
        SignInRequest invalidRequest = new SignInRequest();
        invalidRequest.setEmail("nonexistent@example.com");
        invalidRequest.setPassword("wrongpassword");

        // When & Then
        assertThatThrownBy(() -> authenticationApi.signIn(invalidRequest))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(4)
    void signUp_WhenDuplicateEmail_ShouldThrowException() {
        // Given - create user first
        authenticationApi.signUp(signUpRequest);

        // When & Then - try to create user with same email
        assertThatThrownBy(() -> authenticationApi.signUp(signUpRequest))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(5)
    void signUp_WhenInvalidEmail_ShouldThrowException() {
        // Given
        SignUpRequest invalidRequest = new SignUpRequest();
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("TestPassword123!");
        invalidRequest.setConfirmPassword("TestPassword123!");
        invalidRequest.setFirstName("Test");
        invalidRequest.setLastName("User");

        // When & Then
        assertThatThrownBy(() -> authenticationApi.signUp(invalidRequest))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(6)
    void signUp_WhenPasswordMismatch_ShouldThrowException() {
        // Given
        SignUpRequest invalidRequest = new SignUpRequest();
        invalidRequest.setEmail("test2@example.com");
        invalidRequest.setPassword("TestPassword123!");
        invalidRequest.setConfirmPassword("DifferentPassword123!");
        invalidRequest.setFirstName("Test");
        invalidRequest.setLastName("User");

        // When & Then
        assertThatThrownBy(() -> authenticationApi.signUp(invalidRequest))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    // ==================== Protected Endpoints (Auth Required) ====================

    @Test
    @Order(10)
    @DirtiesContext
    void getCurrentUserProfile_WhenAuthenticated_ShouldReturnUserInfo() {
        // Given - create user and authenticate
        createUserAndAuthenticate();

        // When
        UserInfo userInfo = profileApi.currentUserInfo();

        // Then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getEmail()).isEqualTo(signUpRequest.getEmail());
        assertThat(userInfo.getFirstName()).isEqualTo("Test");
        assertThat(userInfo.getLastName()).isEqualTo("User");
        assertThat(userInfo.getUserId()).isNotNull();
    }

    @Test
    @Order(11)
    @DirtiesContext
    void updateCurrentUserProfile_WhenAuthenticated_ShouldUpdateProfile() {
        // Given - create user and authenticate
        createUserAndAuthenticate();

        UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setPhoneNumber("+1234567890");

        // When
        UserInfo updatedUserInfo = profileApi.updateCurrentUserProfile(updateRequest);

        // Then
        assertThat(updatedUserInfo).isNotNull();
        assertThat(updatedUserInfo.getFirstName()).isEqualTo("Updated");
        assertThat(updatedUserInfo.getLastName()).isEqualTo("Name");
        assertThat(updatedUserInfo.getPhoneNumber()).isEqualTo("+1234567890");
    }

    @Test
    @Order(12)
    @DirtiesContext
    void updateCurrentUserProfile_WhenInvalidPhoneNumber_ShouldThrowException() {
        // Given - create user and authenticate
        createUserAndAuthenticate();

        UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest();
        updateRequest.setPhoneNumber("invalid-phone-number");

        // When & Then
        assertThatThrownBy(() -> profileApi.updateCurrentUserProfile(updateRequest))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(13)
    @DirtiesContext
    void changeOwnPassword_WhenAuthenticated_ShouldChangePassword() {
        // Given - create user and authenticate
        createUserAndAuthenticate();

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setOldPassword("TestPassword123!");
        changePasswordRequest.setNewPassword("NewPassword123!");
        changePasswordRequest.setConfirmPassword("NewPassword123!");

        // When
        SuccessfulResponse response = profileApi.changeOwnPassword(changePasswordRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isTrue();

        // Verify old password no longer works
        assertThatThrownBy(() -> authenticationApi.signIn(signInRequest))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));

        // Verify new password works
        SignInRequest newSignInRequest = new SignInRequest();
        newSignInRequest.setEmail(signUpRequest.getEmail());
        newSignInRequest.setPassword("NewPassword123!");
        
        TokenResponse newTokenResponse = authenticationApi.signIn(newSignInRequest);
        assertThat(newTokenResponse.getAccessToken()).isNotNull();
    }

    @Test
    @Order(14)
    @DirtiesContext
    void changeOwnPassword_WhenWrongOldPassword_ShouldThrowException() {
        // Given - create user and authenticate
        createUserAndAuthenticate();

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setOldPassword("WrongOldPassword!");
        changePasswordRequest.setNewPassword("NewPassword123!");
        changePasswordRequest.setConfirmPassword("NewPassword123!");

        // When & Then
        assertThatThrownBy(() -> profileApi.changeOwnPassword(changePasswordRequest))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(15)
    @DirtiesContext
    void changeOwnPassword_WhenPasswordMismatch_ShouldThrowException() {
        // Given - create user and authenticate
        createUserAndAuthenticate();

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setOldPassword("TestPassword123!");
        changePasswordRequest.setNewPassword("NewPassword123!");
        changePasswordRequest.setConfirmPassword("DifferentPassword123!");

        // When & Then
        assertThatThrownBy(() -> profileApi.changeOwnPassword(changePasswordRequest))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(16)
    @DirtiesContext
    void refreshToken_WhenValidRefreshToken_ShouldReturnNewAccessToken() {
        // Given - create user and authenticate
        TokenResponse tokenResponse = createUserAndAuthenticate();
        // need refreshoken to refresh token :)
        apiClient.setBearerToken(tokenResponse.getRefreshToken());

        // When
        AccessTokenResponse response = authenticationApi.refreshToken();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getAccessToken()).isNotEqualTo(tokenResponse.getAccessToken());
    }

    @Test
    @Order(17)
    @DirtiesContext
    void signOut_WhenAuthenticated_ShouldSignOutUser() {
        // Given - create user and authenticate
        TokenResponse tokenResponse = createUserAndAuthenticate();
        // need refreshoken to sign out
        apiClient.setBearerToken(tokenResponse.getRefreshToken());

        // When
        SuccessfulResponse response = authenticationApi.signOut();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isTrue();
    }

    @Test
    @Order(20)
    @DirtiesContext
    void getAllUsers_WhenAuthenticated_ShouldReturnUsersList() {
        // Given - create user and authenticate
        createUserAndAuthenticate();

        // When
        UsersPagedResponse response = userApi.getAllUsers(1, 10, null, null, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getTotalElements()).isGreaterThan(0);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getPageSize()).isEqualTo(10);
    }

    @Test
    @Order(21)
    @DirtiesContext
    void getUserById_WhenValidId_ShouldReturnUser() {
        // Given - create user and authenticate
        createUserAndAuthenticate();
        UserInfo currentUser = profileApi.currentUserInfo();
        UUID userId = currentUser.getUserId();

        // When
        UserInfo userInfo = userApi.getUserById(userId);

        // Then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getEmail()).isEqualTo(signUpRequest.getEmail());
    }

    @Test
    @Order(22)
    @DirtiesContext
    void getUserById_WhenInvalidId_ShouldThrowException() {
        // Given - create user and authenticate
        createUserAndAuthenticate();
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> userApi.getUserById(nonExistentId))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(30)
    void apiCalls_WhenNotAuthenticated_ShouldReturn403() {
        // When & Then - try to access protected endpoints without authentication
        assertThatThrownBy(() -> profileApi.currentUserInfo())
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));

        assertThatThrownBy(() -> userApi.getAllUsers(1, 10, null, null, null))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));

        assertThatThrownBy(() -> userApi.getUserById(UUID.randomUUID()))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(401));
    }
}

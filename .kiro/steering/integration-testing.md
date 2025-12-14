---
inclusion: fileMatch
fileMatchPattern: "**/*IntegrationTest.java"
---

# Integration Testing Guidelines - Core Microservices

## Overview
Integration tests verify the full request/response cycle through the actual HTTP layer using generated API clients (`*Api` classes) instead of raw `RestClient`.

## Test Structure

### Use Generated API Clients
```java
// ✅ Correct - Use generated API clients
private AuthenticationApi authenticationApi;
private ProfileApi profileApi;
private UserApi userApi;

@BeforeEach
void setUp() {
    String baseUrl = "http://localhost:" + port;
    RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
    ApiClient apiClient = new ApiClient(restClient);
    
    authenticationApi = new AuthenticationApi(apiClient);
    profileApi = new ProfileApi(apiClient);
    userApi = new UserApi(apiClient);
}

// ❌ Wrong - Don't use raw RestClient for API calls
userRestClient.post()
    .uri(baseUrl + "/api/auth/signup")
    .body(signUpRequest)
    .retrieve()
    .body(SuccessfulResponse.class);
```

### Authentication in Integration Tests

**CRITICAL**: `@WithMockUser` does NOT work with API clients making real HTTP requests. The mock security context doesn't propagate to HTTP calls.

```java
// ✅ Correct - Use real authentication flow
private TokenResponse createUserAndAuthenticate() {
    authenticationApi.signUp(signUpRequest);
    TokenResponse tokenResponse = authenticationApi.signIn(signInRequest);
    apiClient.setBearerToken(tokenResponse.getAccessToken());
    return tokenResponse;
}

@Test
void protectedEndpoint_WhenAuthenticated_ShouldWork() {
    createUserAndAuthenticate();
    UserInfo userInfo = profileApi.currentUserInfo();
    assertThat(userInfo).isNotNull();
}

// ❌ Wrong - @WithMockUser doesn't work with HTTP API clients
@WithMockUser(username = "testuser", roles = {"USER_MS_USER"})
void protectedEndpoint_WhenAuthenticated_ShouldWork() {
    // This will get 401 - mock user context not sent in HTTP request
    UserInfo userInfo = profileApi.currentUserInfo();
}
```

### Public vs Protected Endpoints

**Public endpoints** (no auth required):
- `/api/auth/signup`
- `/api/auth/signin`
- Test directly without authentication setup

**Protected endpoints** (auth required):
- Call `createUserAndAuthenticate()` first
- Token is set on `ApiClient` via `setBearerToken()`

## Exception Handling

Generated API clients throw `RestClientResponseException` (not `ApiException`):

```java
assertThatThrownBy(() -> authenticationApi.signIn(invalidRequest))
    .isInstanceOf(RestClientResponseException.class)
    .satisfies(ex -> assertThat(((RestClientResponseException) ex)
        .getStatusCode().value()).isEqualTo(400));
```

## Test Annotations

```java
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
    private UserApi userApi;

    @BeforeEach
    void setUp() {
        // Set the base path to the random port
        apiClient.setBasePath("http://localhost:" + port);
    }
    
    @Test
    @Order(1)
    void testMethod() { }
}
```

**Important**: Always use `RANDOM_PORT` to avoid port conflicts when running multiple test classes. Inject `@LocalServerPort` and set the `ApiClient` base path in `@BeforeEach`.

## Roles

Use roles from `CoreMsRoles` enum:
- `USER_MS_ADMIN`, `USER_MS_USER`
- `COMMUNICATION_MS_ADMIN`, `COMMUNICATION_MS_USER`
- `TRANSLATION_MS_ADMIN`
- `DOCUMENT_MS_ADMIN`, `DOCUMENT_MS_USER`
- `SYSTEM`, `SUPER_ADMIN`

## Test Organization

Group tests by endpoint type:
1. **Public Endpoints** (Orders 1-9): No auth required
2. **Protected Endpoints** (Orders 10-29): Require authentication
3. **Unauthorized Access Tests** (Order 30+): Verify 401 responses

## ApiClient Configuration

The `ApiClient` class provides:
- `setBearerToken(String token)` - Set auth token for requests
- `setBasePath(String basePath)` - Override base URL
- `addDefaultHeader(String name, String value)` - Add custom headers

## Common Patterns

### Test Data Setup
```java
@BeforeEach
void setUp() {
    // Set base path for random port
    apiClient.setBasePath("http://localhost:" + port);
    
    // Use unique email per test to avoid conflicts
    String uniqueEmail = "testuser" + System.currentTimeMillis() + "@example.com";
    signUpRequest = new SignUpRequest();
    signUpRequest.setEmail(uniqueEmail);
    signUpRequest.setPassword("TestPassword123!");
    // ...
}
```

### Verifying Error Responses
```java
@Test
void endpoint_WhenInvalidInput_ShouldReturn400() {
    createUserAndAuthenticate();
    
    assertThatThrownBy(() -> profileApi.updateCurrentUserProfile(invalidRequest))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex)
            .getStatusCode().value()).isEqualTo(400));
}
```

### Verifying Unauthorized Access
```java
@Test
void protectedEndpoint_WhenNotAuthenticated_ShouldReturn401() {
    // No authentication setup - apiClient has no bearer token
    assertThatThrownBy(() -> profileApi.currentUserInfo())
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex)
            .getStatusCode().value()).isEqualTo(401));
}
```

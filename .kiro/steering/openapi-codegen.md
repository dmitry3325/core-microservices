---
inclusion: fileMatch
fileMatchPattern: "**/*-api/**"
---

# OpenAPI & Code Generation

## OpenAPI Checklist
- Place spec at: `*-api/src/main/resources/<service>-api.yaml`
- Include `servers:` section
- Every operation MUST have explicit `operationId` (camelCase)
- Reuse `.gen/common-api.yaml` components
- Add validation constraints in schema
- Run codegen + compile before implementing logic

## POM Guidance
- `*-api` POM: depend on `com.corems.common:api`
- Configure `maven-dependency-plugin` to unpack common API resources
- Configure `openapi-generator-maven-plugin` (no versions in child POMs)

## Build Commands
```bash
# Generate API models
mvn -pl <api-module-path> -am clean install -DskipTests=true
```

## Generated Client Structure
The `*-client` module provides:
- `ApiClient` - HTTP client with auth support
- `*Api` classes - Type-safe API methods (e.g., `AuthenticationApi`, `ProfileApi`, `UserApi`)
- Auto-configuration via `*MsClientConfig`

### ApiClient Usage
```java
RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
ApiClient apiClient = new ApiClient(restClient);
apiClient.setBearerToken(accessToken);  // Set auth token

AuthenticationApi authApi = new AuthenticationApi(apiClient);
ProfileApi profileApi = new ProfileApi(apiClient);
```

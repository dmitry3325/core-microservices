# Core Microservices — Autoconfig-first LLM Instructions

This is a short, prescriptive guide for generating and implementing Core Microservices. It assumes a clean, autoconfiguration-first workflow: services depend on the shared auto-config starter and do not add legacy `@Enable*` annotations.

## Quick Intent

- Prefer the shared auto-configuration starter on the classpath. Service application classes should be minimal and should NOT declare `@EnableCoreMs*` annotations.
- Use the starter and documented replacement patterns only when intentionally opting out.

## Platform & Conventions

- Java 25, Spring Boot 4, Maven multi-module reactor
- PostgreSQL database, JWT Authentication
- RabbitMQ / Apache Kafka for messaging
- Shared code lives in `common/`. Do NOT edit `common` in service PRs.
- Generated API models (from `*-api`) are canonical DTOs. Do NOT duplicate them locally.

## Folder Layout

```
<service>-ms/
  <service>-api/          # OpenAPI spec + generated models (DO NOT implement logic here)
  <service>-client/       # ApiClient auto-config for other services
  <service>-service/      # Implementation
    src/main/java/.../app/
      controller/
      service/
      repository/
      entity/
      config/
    src/main/resources/
      application.yaml
      db-config.yaml
      security-config.yaml
```

## Autoconfiguration-First Rules

- Include shared auto-config starter: `com.corems.common:autoconfig-starter`
- Also add `com.corems.common:logging` in service POM for compile-time resolution
- Starter wires logging, error handling, security automatically
- If excluding shared security, document why and provide replacement beans

## Roles (CoreMsRoles enum)

```java
// User Microservice
USER_MS_ADMIN, USER_MS_USER

// Communication Microservice
COMMUNICATION_MS_ADMIN, COMMUNICATION_MS_USER

// Translation Microservice
TRANSLATION_MS_ADMIN

// Document Microservice
DOCUMENT_MS_ADMIN, DOCUMENT_MS_USER

// System roles
SYSTEM, SUPER_ADMIN
```

## Security Rules

- Use `com.corems.common.security.SecurityUtils`:
  - `SecurityUtils.getUserPrincipal()` - returns UserPrincipal or throws UNAUTHORIZED
  - `SecurityUtils.getUserPrincipalOptional()` - returns Optional<UserPrincipal>
- Resolve identity from Spring Security only; cast to `UserPrincipal` in service layer
- Do NOT rely on X-User or client-supplied headers
- Use `@RequireRoles(CoreMsRoles.<SERVICE>_ADMIN)` for role-gated operations

## OpenAPI Checklist

- Place spec at: `*-api/src/main/resources/<service>-api.yaml`
- Include `servers:` section
- Every operation MUST have explicit `operationId` (camelCase)
- Reuse `.gen/common-api.yaml` components for shared responses/parameters
- Add validation constraints in schema (pattern, minLength/maxLength)
- Run codegen + compile before implementing logic

## Entities & Lombok

- Use: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Use: `@EqualsAndHashCode(onlyExplicitlyIncluded=true)` with id included
- Avoid `@Data` on JPA entities
- Repositories MUST implement `SearchableRepository<T,ID>`

## Integration Testing

**CRITICAL**: `@WithMockUser` does NOT work with generated API clients making real HTTP requests.

Use real authentication flow:
```java
private TokenResponse createUserAndAuthenticate() {
    authenticationApi.signUp(signUpRequest);
    TokenResponse tokenResponse = authenticationApi.signIn(signInRequest);
    apiClient.setBearerToken(tokenResponse.getAccessToken());
    return tokenResponse;
}
```

Generated API clients throw `RestClientResponseException` (not `ApiException`):
```java
assertThatThrownBy(() -> authenticationApi.signIn(invalidRequest))
    .isInstanceOf(RestClientResponseException.class)
    .satisfies(ex -> assertThat(((RestClientResponseException) ex)
        .getStatusCode().value()).isEqualTo(400));
```

## Code Style

### Import Style
- Prefer explicit imports for generated models and nested enums
- Example: `import com.corems.communicationms.api.model.MessageResponse.SentByTypeEnum;`

### Commenting Policy
- Keep code self-explanatory; comments are exceptional
- Allow: short rationale, links to issues/specs, brief markers for logical blocks
- Remove: comments that restate code (`// set userId`, `// populate sender info`)
- Use Javadoc for public APIs (required for controller/service methods)
- Tag actionable items: `TODO:` / `FIXME:` with owner/ticket

## Generation Phases (MANDATORY pauses)

### Phase 1 — API & Skeleton (STOP for review)
1. Create `*-api` spec and POM with codegen
2. Create `*-service/pom.xml` and minimal application class
3. Add config files: `application.yaml`, `db-config.yaml`, `security-config.yaml`
4. Run: `mvn -pl <api-module-path> -am clean install -DskipTests=true`
5. **STOP for human review**

### Phase 2 — Entities & Repositories (STOP for review)
1. Implement JPA entities and repositories
2. Repositories implement `SearchableRepository<T,ID>` with metadata methods
3. Map DTOs: Integer -> Long for ids, OffsetDateTime -> Instant
4. Run: `mvn -pl <service-module-path> -am clean install -DskipTests=true`
5. **STOP for human review**

### Phase 3 — Controllers, Services, Tests
1. Implement controllers using generated API interfaces
2. Business logic in service layer
3. Add integration tests using real auth flow (not @WithMockUser)
4. Run full build with tests

## PR Checklist

- Avoided editing `common`
- Used generated models instead of local DTOs
- Ran API codegen + build for `*-api`
- Avoided re-declaring parent-managed plugins
- Did not commit `.gen` files

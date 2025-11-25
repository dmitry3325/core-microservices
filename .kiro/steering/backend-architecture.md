---
inclusion: always
---

# Backend Architecture - Core Microservices

## Platform
- Java 17, Spring Boot 3.3+
- Maven multi-module reactor
- PostgreSQL database
- JWT Authentication
- RabbitMQ / Apache Kafka

## Autoconfiguration-First Approach

### Preferred Pattern
- Include shared auto-config starter: `com.corems.common:autoconfig-starter`
- Service application classes should be minimal
- DO NOT add `@EnableCoreMs*` annotations
- Starter wires logging, error handling, security automatically

### Dependencies
- Add `com.corems.common:logging` in service POM for compile-time resolution
- If excluding shared security, document why and provide replacement beans

## Folder Layout
```
<service>-ms/
├── <service>-api/          # OpenAPI spec + generated models
├── <service>-client/       # ApiClient auto-config
└── <service>-service/      # Implementation
    └── src/main/java/.../app/
        ├── controller/
        ├── service/
        ├── repository/
        ├── entity/
        └── config/
    └── src/main/resources/
        ├── application.yaml
        ├── db-config.yaml
        └── security-config.yaml
```

## Code Style

### Import Style
- Prefer explicit imports for generated models and nested enums
- Example: `import com.corems.communicationms.api.model.MessageResponse.SentByTypeEnum;`

### Commenting Policy (Strict)
**Goal**: Keep code self-explanatory. Comments are exceptional.

**Allowed comments inside method bodies**:
1. Short rationale for non-obvious decisions (1-2 lines)
2. Links to external issues/specs for workarounds
3. Brief markers for logical blocks in long methods (prefer refactoring)

**Remove all comments that restate code**:
- ❌ `// set userId`
- ❌ `// populate sender info`
- ❌ `// set the response`

**Formatting**:
- Use Javadoc for public APIs (required for controller/service methods)
- Use `//` for short rationale only
- Tag actionable items: `TODO:` / `FIXME:` with owner/ticket

**Examples**:
- ✅ `// Prefer DB-generated UUID to avoid distributed coordination (see DOC-456)`
- ❌ `// set createdAt on the response`

## Microservice Generation Phases

### Phase 1 - API & Skeleton (STOP for review)
- Create `*-api` spec and `*-api/pom.xml` with codegen
- Create `*-service/pom.xml` (minimal)
- Create application class relying on auto-config starter
- Add `application.yaml`, `db-config.yaml`, `security-config.yaml`, `.env-example`, `README.md`
- Run: `mvn -pl <api-module-path> -am clean install -DskipTests=true`
- **STOP for human review**

### Phase 2 - Entities & Repositories (STOP for review)
- Implement JPA entities and repositories
- Repositories MUST implement `SearchableRepository<T,ID>`
- Expose metadata: `getSearchFields()`, `getAllowedFilterFields()`, `getAllowedSortFields()`, `getFieldAliases()`
- Map DTOs carefully: Integer -> Long for ids, OffsetDateTime -> Instant
- Run: `mvn -pl <service-module-path> -am clean install -DskipTests=true`
- **STOP for human review**

### Phase 3 - Controllers, Services, Tests
- Implement controllers using generated API interfaces
- Business logic in service layer
- Use `RequireRoles(CoreMsRoles.<SERVICE>_ADMIN)` for role-gated operations
- Implement paginated listing with `PaginatedQueryExecutor.execute(...)`
- Add tests and run full build

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
- `*-service` POM: keep minimal, add runtime dependencies

## Entities & Lombok
- Use: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Use: `@EqualsAndHashCode(onlyExplicitlyIncluded=true)` with id included
- Avoid `@Data` on JPA entities

## Security Rules

### Identity Resolution
- Use `com.corems.common.security.SecurityUtils` helper:
  - `SecurityUtils.getUserPrincipal()` - returns UserPrincipal or throws UNAUTHORIZED
  - `SecurityUtils.getUserPrincipalOptional()` - returns Optional<UserPrincipal>
- Resolve identity from Spring Security only
- Cast to `UserPrincipal` in service layer
- DO NOT rely on X-User or client-supplied headers

### Sender Identity (Messages/Notifications)
Populate sender metadata in service layer at creation:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (auth != null && auth.getPrincipal() instanceof UserPrincipal up && up.getUserId() != null) {
    entity.setSentById(up.getUserId());
    entity.setSentByType(MessageSenderType.user);
} else {
    entity.setSentByType(MessageSenderType.system);
    // Do NOT populate sentById
}
```

## Testing
- Include starter on test classpath or use `@ImportAutoConfiguration`
- Use `@MockBean` for shared beans in slice tests
- Mock `Authentication`/`UserPrincipal` from SecurityContext

## PR Checklist
- ✅ Avoided editing `common`
- ✅ Used generated models instead of local DTOs
- ✅ Ran API codegen + build for `*-api`
- ✅ Avoided re-declaring parent-managed plugins
- ✅ Did not commit `.gen` files

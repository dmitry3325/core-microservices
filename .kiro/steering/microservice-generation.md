---
inclusion: manual
---

# Microservice Generation Phases

## Phase 1 - API & Skeleton (STOP for review)
1. Create `*-api` spec and `*-api/pom.xml` with codegen
2. Create `*-service/pom.xml` (minimal)
3. Create application class relying on auto-config starter
4. Add config files:
   - `application.yaml`
   - `db-config.yaml`
   - `security-config.yaml`
   - `.env-example`
   - `README.md`
5. Run: `mvn -pl <api-module-path> -am clean install -DskipTests=true`
6. **STOP for human review**

## Phase 2 - Entities & Repositories (STOP for review)
1. Implement JPA entities and repositories
2. Repositories MUST implement `SearchableRepository<T,ID>`
3. Expose metadata: `getSearchFields()`, `getAllowedFilterFields()`, `getAllowedSortFields()`, `getFieldAliases()`
4. Map DTOs carefully: Integer -> Long for ids, OffsetDateTime -> Instant
5. Run: `mvn -pl <service-module-path> -am clean install -DskipTests=true`
6. **STOP for human review**

## Phase 3 - Controllers, Services, Tests
1. Implement controllers using generated API interfaces
2. Business logic in service layer
3. Use `RequireRoles(CoreMsRoles.<SERVICE>_ADMIN)` for role-gated operations
4. Implement paginated listing with `PaginatedQueryExecutor.execute(...)`
5. Add integration tests (see integration-testing.md)
6. Run full build

## POM Guidance

### *-api POM
- Depend on `com.corems.common:api`
- Configure `maven-dependency-plugin` to unpack common API resources
- Configure `openapi-generator-maven-plugin` (no versions in child POMs)

### *-service POM
- Keep minimal
- Add runtime dependencies only
- Include `com.corems.common:autoconfig-starter`

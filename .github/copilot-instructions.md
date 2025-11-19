# Core Microservices — Autoconfig-first LLM Instructions

This is a short, prescriptive guide for generating and implementing Core Microservices. It assumes a clean, autoconfiguration-first workflow: services depend on the shared auto-config starter and do not add legacy `@Enable*` annotations.

Quick intent
- Prefer the shared auto-configuration starter on the classpath. Service application classes should be minimal and should NOT declare `@EnableCoreMs*` annotations. Use the starter and documented replacement patterns only when intentionally opting out.

Folder layout (always verify before editing)
- <service>-ms/
  - <service>-api/      → OpenAPI spec + generated models/interfaces (DO NOT implement logic here)
  - <service>-client/   → ApiClient auto-config for other services
  - <service>-service/  → Implementation (controllers, services, entities, repos)
    - src/main/java/.../app/
      - controller/, service/, repository/, entity/, config/
    - src/main/resources/ → application.yaml, db-config.yaml, security-config.yaml

Platform & conventions
- Java 17, Spring Boot 3.3+, Maven multi-module reactor.
- Shared code lives in `common/`. Do NOT edit `common` in service PRs.
- Generated API models (from `*-api`) are canonical DTOs. Do NOT duplicate them locally.
- Parent POM manages dependency and plugin versions. Child modules MUST NOT redeclare parent-managed plugin versions.
- Never commit `.gen` or generated artifacts that duplicate `common/api`.

Autoconfiguration-first rules (short)
- Preferred dependency: include the shared auto-config starter on the service classpath (artifact example: `com.corems.common:autoconfig-starter`). The starter wires logging, error handling, security and other cross-cutting beans.
- Also add `com.corems.common:logging` in the service POM so logging-related classes resolve at compile time (parent manages versions).
- If a service intentionally excludes the shared security auto-config, document why and provide replacement beans (e.g., `TokenProvider`). Replacements MUST be implemented in the service module and documented in the application class.

POM guidance
- `*-api` POM: depend on `com.corems.common:api` (no hardcoded version in reactor). Configure `maven-dependency-plugin` to unpack common API resources and `openapi-generator-maven-plugin` generate execution (do NOT specify plugin versions in child POMs).
- `*-service` POM: keep minimal. Add runtime dependencies (spring starters, data-jpa, validation, log4j2, db driver) and `com.corems.common:service-exception`, `com.corems.common:security`, `com.corems.common:db-utils`, `com.corems.common:logging` as needed.

OpenAPI checklist (must follow)
- Place spec at: `*-api/src/main/resources/<service>-api.yaml`.
- Include `servers:` section to silence generator warnings.
- Every operation MUST have an explicit `operationId` (stable, camelCase).
- Reuse `.gen/common-api.yaml` components for shared responses/parameters (errors, pagination).
- Add validation constraints in the schema (pattern, minLength/maxLength) for fields that require them.
- After creating/updating the API spec run codegen + compile before implementing service logic.

Microservice generation phases (MANDATORY pauses)
- Phase 1 — API & app skeleton (STOP after this phase for human review):
  - Create `*-api` spec and `*-api/pom.xml` with codegen wiring.
  - Create `*-service/pom.xml` (minimal) and an application class that RELIES on the shared auto-config starter (do NOT add `@Enable*`).
  - Add `application.yaml`, `db-config.yaml`, `security-config.yaml` (if using security), `.env-example`, `README.md` and Java package folders.
  - Run: `mvn -pl <api-module-path> -am clean install -DskipTests=true` and fix generator/compile issues.
  - STOP and request human review of the API + skeleton before continuing.

- Phase 2 — Entities & Repositories (STOP after this phase for human review):
  - Implement JPA entities and repositories only. Repositories MUST implement `com.corems.common.utils.db.repo.SearchableRepository<T,ID>` and expose metadata methods: `getSearchFields()`, `getAllowedFilterFields()`, `getAllowedSortFields()`, `getFieldAliases()`.
  - Map generated DTOs to JPA carefully (Integer -> Long for ids, OffsetDateTime -> Instant for timestamps).
  - Run: `mvn -pl <service-module-path> -am clean install -DskipTests=true` to validate compilation.
  - STOP and request human review of the data model and repository metadata.

- Phase 3 — Controllers, Services, Wiring & Tests:
  - Implement controllers by wiring generated API interfaces from `*-api` (do not change generated signatures).
  - Business logic goes into service layer; resolve identity from SecurityContext (`Authentication auth = SecurityContextHolder.getContext().getAuthentication()` and cast principal to `UserPrincipal`). Do NOT use client-supplied headers for identity.
  - Use `RequireRoles(CoreMsRoles.<SERVICE>_ADMIN)` on controller classes for role-gated operations.
  - Implement paginated listing using `PaginatedQueryExecutor.execute(...)` where applicable.
  - Add unit/integration tests and run full module build with tests.

Entities & Lombok rules
- Use Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(onlyExplicitlyIncluded=true)` with id included. Avoid `@Data` on JPA entities.
- Map DTO -> entity types explicitly as needed.

Security rules (short)
- Use `com.corems.common.security` and `RequireRoles` for controller-level checks.
- Resolve identity from Spring Security only; cast to `UserPrincipal` in service layer.
- Do NOT rely on X-User or any client-supplied headers.

Testing guidance
- For tests that need shared auto-config, include the starter on the test classpath or use `@ImportAutoConfiguration(...)`.
- For slice tests that should mock shared beans, use `@MockBean` for those beans. Mock `Authentication`/`UserPrincipal` from `SecurityContext` for identity.

CI / pre-commit checks (recommended)
- Add checks that:
  - Fail the commit if `.gen` or generated API files are included.
  - Run `mvn -pl <changed-api> -am -DskipTests=true validate` for changed API modules.
  - Optionally grep for accidental `@EnableCoreMs*` annotations when the shared starter is present.

Quick PR checklist
- Did I avoid editing `common`? YES/NO
- Did I rely on generated models instead of creating local DTOs? YES/NO
- Did I run API codegen + build for the `*-api`? YES/NO
- Did I avoid re-declaring parent-managed plugins in child POMs? YES/NO
- Did I not commit `.gen`? YES/NO

Minimal service skeleton to generate
- `<service>-ms/`
  - `<service>-api/` (OpenAPI + codegen wiring)
  - `<service>-client/` (ApiClient auto-config)
  - `<service>-service/` (implementation)
    - `src/main/java/com/corems/<servicerms>/app/<Service>ServiceApplication.java` (main class — minimal; rely on auto-config starter)
    - `src/main/resources/application.yaml`, `db-config.yaml`, `security-config.yaml`

Notes & assumptions
- Assumes a shared auto-config starter artifact is available (example: `com.corems.common:autoconfig-starter`). If the real artifact name differs, replace it in POM guidance.
- This document is intentionally concise — prefer the starter/import pattern and stop at each generation phase for review.

If you'd like I will:
- Replace the current repository instruction file with this content (done if you confirm),
- Create a short `docs/autoconfig.md` example showing the preferred pom dependency and a minimal application class, and
- Add a small grep-based pre-commit script under `.git/hooks/pre-commit.sample` that enforces `.gen` blocking and starter/annotation mismatches.

// End of file

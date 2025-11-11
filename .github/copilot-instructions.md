# Core Microservices — Short LLM Instructions

This file is a compact, machine-friendly prompt for automated code generation. Keep it short, prescriptive, and literal.

Quick summary (must follow exactly)
- Platform: Java 17, Spring Boot 3.3+, Maven multi-module.
- Always use `common` for shared code. Do NOT edit `common` sources in service tasks.
- Generated API models are canonical DTOs (from `*-api`). Do NOT create duplicate local DTOs.
- Parent POM manages versions and build plugins. Child modules MUST NOT re-declare or configure plugins that are managed by the parent (e.g., `org.springframework.boot:spring-boot-maven-plugin`).
- Do NOT commit `.gen` or generated artifacts that duplicate `common/api`.

Strict rules (enforced)
- GroupId consistency: `*-api` groupId must match the aggregator module groupId (e.g., `com.corems.translationms:translation-api`).
- Use generated models (package `com.<module>.model`) in controllers/services. When mapping to JPA entities convert ids/timestamps explicitly: Integer -> Long, OffsetDateTime -> Instant.
- Repos: implement `com.corems.common.utils.db.repo.SearchableRepository<T,ID>` and expose `getSearchFields()`, `getAllowedFilterFields()`, `getAllowedSortFields()`, `getFieldAliases()`.
- Security:
  - Use `common/security`. Do NOT use client-supplied headers as identity.
  - For controller-level role checks use `com.corems.common.security.RequireRoles` (example: `@RequireRoles(CoreMsRoles.TRANSLATION_MS_ADMIN)`). Do NOT use `@PreAuthorize` or SpEL-based annotations.
  - Resolve identity from Spring Security context only (e.g., `Authentication auth = SecurityContextHolder.getContext().getAuthentication()` and cast principal to `UserPrincipal`).
- Lombok: do not add module-level Lombok versions; centralize in parent dependencyManagement. Do NOT use `@Data` on JPA entities; prefer `@Getter`/`@Setter` and `@EqualsAndHashCode(onlyExplicitlyIncluded=true)` with id included.
- Keep service POMs minimal: avoid plugin declarations managed by parent and add `com.corems.common:logging` dependency when using `@EnableCoreMsLogging` to make annotations resolve at compile time.

Service application annotations (required):
- All service main application classes MUST enable the shared cross-cutting auto-configuration by using the following annotations on the application class:
  - `@EnableCoreMsLogging` (enables shared logging helpers)
  - `@EnableCommonErrorHandling` (enables shared exception handling)
  - `@EnableCoreMsSecurity` (enables shared security filters and beans)
- Do NOT manually declare a `TokenProvider` (or other security beans provided by `@EnableCoreMsSecurity`) in services that use `@EnableCoreMsSecurity`. The only exception is services that explicitly exclude the shared security auto-config (for example `user-ms` currently excludes `SecurityAutoConfiguration` and declares its own TokenProvider).
- When using `@EnableCoreMsLogging` add a dependency on `com.corems.common:logging` in the service POM so the annotation and auto-config classes resolve at compile time (no version in child POM; parent manages versions).

OpenAPI YAML checklist (must follow)
- File path: `src/main/resources/<service>-api.yaml` inside `*-api` module.
- Always include a `servers:` section (e.g. `servers:\n  - url: http://localhost`). This prevents generator "servers/host" messages.
- Avoid multiple anonymous inline `allOf` objects. If extending a referenced schema, use a single inline `type: object` with `required`/`properties` or create a named component schema and reference it.
- Discriminator mappings: map values to named `#/components/schemas/*` components.
- Required dependencies in `*-api` POM: `com.corems.common:api` (no hardcoded version in reactor), `io.swagger.core.v3:swagger-core-jakarta` (omit version if parent-managed).
- Build plugin wiring in `*-api` (enforced): add `maven-dependency-plugin` unpack execution id `unpack-common-openapi` and `openapi-generator-maven-plugin` generate execution (do NOT specify a plugin `<version>` in child POM; pluginManagement handles versions).
- Use common shared API components: every `*-api` spec MUST reference standard responses/parameters/pagination from the unpacked `com.corems.common:api` artifact available at `.gen/common-api.yaml`. Examples:
   - `<<: *common-error-responses` pattern (see `user-ms` for example)
   - `$ref: '.gen/common-api.yaml#/components/parameters/page'`
   - `$ref: '.gen/common-api.yaml#/components/schemas/PaginationMeta'`
   This keeps error responses, pagination metadata and common parameters consistent across services.
   - API error responses required: each operation MUST include or inherit references for common error responses: 400 (ValidationError), 401 (UnauthorizedError), 403 (ForbiddenError), 404 (NotFoundError), 500 (InternalServerError) by referencing `.gen/common-api.yaml#/components/responses/*` or using the shared `x-common-error-responses` anchor pattern used in `user-ms`.
 - Operation IDs: every operation object MUST include an explicit `operationId` (use stable, camelCase names) so generated code and clients have predictable method names.
 - API generation pause for review: when generating a new microservice, after the `*-api` spec is created and API codegen + compile completes successfully, STOP and ask for a human review before implementing the service module. This prevents wasted work when the API contract needs changes.

API generation action pattern (LLM -> code)
1. Create/modify `src/main/resources/<service>-api.yaml` (follow OpenAPI YAML checklist).
2. Add dependency on `com.corems.common:api` in `*-api` (no version when building in the reactor).
3. Ensure `maven-dependency-plugin` unpack (`unpack-common-openapi`) and `openapi-generator-maven-plugin` generate execution are present in the `*-api` module POM (no `<version>`).
4. Run codegen + compile to validate: `mvn -pl <api-module-path> -am clean install -DskipTests=true` from the repo root.
5. If generator prints warnings (common ones below), fix the YAML and re-run:
   - "'host' (OAS 2.0) or 'servers' (OAS 3.0) not defined" -> add `servers`.
   - "allOf with multiple schemas defined. Using only the first one" -> consolidate inline allOf or create named components.

Service config checklist (must be added for each service)
- Add `application.yaml` under `<service>-service/src/main/resources` with minimal settings: `server.port`, `spring.application.name`, and `spring.config.import: classpath:db-config.yaml, security-config.yaml`.
- Add `db-config.yaml` to read DB envs: `spring.datasource.url=${DATABASE_URL}`, `username=${DATABASE_USER}`, `password=${DATABASE_PASSWORD}`, set `hibernate.ddl-auto` as appropriate (dev: update).
- Add `security-config.yaml` to configure JWT/secrets and cors (point to `${AUTH_TOKEN_SECRET}` and TTLs).
- Add `.env-example` at service root listing mandatory env vars: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `AUTH_TOKEN_SECRET`, and any service-specific optional vars (e.g., `ALLOWED_DOMAINS`, `CACHE_TTL_SECONDS`).
- Optionally add `.env` for local dev with safe defaults; DO NOT commit production secrets.

Build & validation rules
- After any API or POM change always run: `mvn -pl <module-path> -am clean install -DskipTests=true` and fix any errors/warnings before committing.
- Avoid committing generated code or `.gen` files that duplicate `com.corems.common:api`.

Minimal example snippets LLM may follow
- Use RequireRoles at class level:
  - `@RequireRoles(CoreMsRoles.TRANSLATION_MS_ADMIN)`
- Resolve principal in service layer:
  - `Authentication auth = SecurityContextHolder.getContext().getAuthentication();`
  - `UserPrincipal u = (UserPrincipal) auth.getPrincipal(); String name = u.getName();`

Short checklist for PR review (LLM should self-check before creating files)
- Did I avoid editing `common`? YES/NO
- Did I rely on generated models instead of creating local DTOs? YES/NO
- Did I run the codegen + build for the `*-api`? YES/NO
- Did I avoid re-declaring parent-managed plugins in child POMs? YES/NO
- Did I not commit `.gen`? YES/NO

Microservice generation checklist (exact steps)
- Purpose: follow these steps exactly when generating a new microservice (for example `translation-ms`) so the project stays consistent and avoids past mistakes.

1) Module layout
- Create `<service>-ms/` aggregator with two modules: `<service>-api` and `<service>-service`.
- GroupId for `*-api` must match aggregator groupId (e.g. `com.corems.translationms:translation-api`).
Note: When *starting* a new service, the generator must produce the initial module skeleton **including**:
 - `*-api/pom.xml` and `*-service/pom.xml` (minimal child POMs that import parent and declare module dependencies).
 - A service application class under `*-service/src/main/java` pre-populated with the required CoreMS annotations: `@EnableCoreMsLogging`, `@EnableCommonErrorHandling`, and `@EnableCoreMsSecurity` (unless the service intentionally excludes the shared security auto-config, e.g., `user-ms`).
 - Required config files in `*-service/src/main/resources`: `application.yaml` (with server.port and import lines), `db-config.yaml` when the module uses a DB, and `security-config.yaml` when the module includes `com.corems.common:security`.
 - A `.env-example` and `README.md` in the service root.
 - Create the Java namespace folder structure under `*-service/src/main/java` matching the module GroupId (example: `com.corems.<servicerms>`). This ensures package layout is ready before any code is added.
 After creating the `*-api` spec and wiring `*-api/pom.xml`, run API codegen and compile, then STOP and request a human review of the API and config skeleton before implementing business logic in `*-service`.

+Generation phases and mandatory pauses (required)
+-----------------------------------------------
+To avoid wasted work and catch API/config issues early, follow these ordered phases when creating a new microservice. After each phase the generator/author MUST stop and request a human review before proceeding to the next phase.
+
+Phase 1 — API + application definition (MANDATORY STOP)
+- Create the `*-api` spec (`src/main/resources/<service>-api.yaml`) following the OpenAPI checklist (servers, operationId, `.gen` refs for common responses/params).
+- Add `*-api/pom.xml` with the dependency on `com.corems.common:api` and the unpack + openapi-generator executions.
+- Create `*-service/pom.xml` (minimal) and the service application class under the correct namespace with these annotations: `@EnableCoreMsLogging`, `@EnableCommonErrorHandling`, `@EnableCoreMsSecurity` (unless explicit exclusion documented).
+- Add required service configs: `application.yaml`, `db-config.yaml` (if DB is needed), `security-config.yaml` (if security dependency is present), `.env-example`, and `README.md`.
+- Create the Java package (namespace) directories under `*-service/src/main/java` (matching the groupId).
+- Run: `mvn -pl <api-module-path> -am clean install -DskipTests=true` and fix any generation/compilation issues.
+
+STOP: pause here and request a human review of the API contract and the service skeleton (application class + configs + package layout). DO NOT implement entities, repositories, controllers, or services until the review approves the API and config.
+
+Phase 2 — Entities & Repositories (MANDATORY STOP)
+- After Phase 1 approval, implement JPA entities and repositories only. Repositories must implement `com.corems.common.utils.db.repo.SearchableRepository<T,ID>` and expose `getSearchFields()`, `getAllowedFilterFields()`, `getAllowedSortFields()`, `getFieldAliases()`.
+- Map generated DTO types to entity types carefully (Integer -> Long, OffsetDateTime -> Instant) and follow Lombok/entity guidelines.
+- Add DB migration scripts if required (optional) and run `mvn -pl <service-module-path> -am clean install -DskipTests=true` to validate.
+
+STOP: pause here and request a human review of the data model and repository search/filter/sort metadata. DO NOT implement controllers/services until this is approved.
+
+Phase 3 — Controllers, Services, wiring and tests
+- After Phase 2 approval, implement controllers (implement generated API interfaces), services (business logic), and repository wiring. Use `RequireRoles` on controller classes for role-gated endpoints.
+- Implement pageable listing using `PaginatedQueryExecutor.execute(...)` where applicable.
+- Add unit/integration tests and run validation: `mvn -pl <service-module-path> -am clean install -DskipTests=false` (or with tests as appropriate).
+
+Only after Phase 3 completes and passes QA should the feature be considered finished.

2) `*-api` (OpenAPI + codegen)
- Add `src/main/resources/<service>-api.yaml` (follow OpenAPI YAML checklist above).
- POM requirements (do NOT add versions managed by parent):
  - dependency: `com.corems.common:api` (no version in reactor builds).
  - dependency: `io.swagger.core.v3:swagger-core-jakarta` (omit version if parent-managed).
  - add `maven-dependency-plugin` unpack execution with id `unpack-common-openapi` that unpacks `com.corems.common:api` into `src/main/resources/.gen`.
  - add `openapi-generator-maven-plugin` generate execution (do NOT specify plugin `<version>`). Configure generator for Spring Boot (interfaceOnly=true, useSpringBoot3=true) and write outputs to `target/generated-sources/openapi`.
- After changes run: `mvn -pl <api-module-path> -am clean install -DskipTests=true` to validate codegen and compilation.

3) `*-service` (implementation)
- POM: keep minimal, do NOT re-declare parent-managed plugins. Add dependencies only (no plugin versions):
  - `com.corems.<servicerms>:<service>-api` (project parent version),
  - `com.corems.common:service-exception`, `com.corems.common:security`, `com.corems.common:db-utils`, `com.corems.common:logging`.
  - Spring starters (web, data-jpa, validation, log4j2) and runtime db driver.
- Do NOT add `<plugin>` entries that are managed by the parent (spring-boot-maven-plugin etc.).

4) Service resources (create these files)
- `translation-ms/translation-service/src/main/resources/application.yaml` with minimal values: `server.port`, `spring.application.name`, and `spring.config.import: classpath:db-config.yaml, security-config.yaml`.
- `db-config.yaml`: map env vars to `spring.datasource.*` and JPA settings.
- `security-config.yaml`: configure `spring.security.jwt.secretKey=${AUTH_TOKEN_SECRET}` and TTLs.
- Service root: `.env-example` listing required envs: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `AUTH_TOKEN_SECRET` and service-specific optional keys (e.g., `ALLOWED_DOMAINS`, `CACHE_TTL_SECONDS`). Add `.env` for local dev only (do NOT commit production secrets).
- `README.md` with quick run steps and mention to run the codegen+build validation after API edits.

5) Controllers/Services/Repos
- Controllers must implement generated API interfaces (do not change generated interface signatures).
- Do not expose JPA entities in controller method signatures — map to/from generated models.
- Use `RequireRoles` for admin endpoints: `@RequireRoles(CoreMsRoles.<SERVICE>_ADMIN)`.
- Resolve authenticated user in the service layer via SecurityContext and `UserPrincipal` (do not use X-User header):
  - `Authentication auth = SecurityContextHolder.getContext().getAuthentication();`
  - `UserPrincipal up = (UserPrincipal) auth.getPrincipal(); String user = up.getName();`
- Repositories must implement `com.corems.common.utils.db.repo.SearchableRepository<T,ID>` and expose metadata methods (getSearchFields, getAllowedFilterFields, getAllowedSortFields, getFieldAliases). Use `PaginatedQueryExecutor.execute(...)` for pageable searches.

6) Entities & mapping
- Use Lombok safely: `@Getter`/`@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with id included. Avoid `@Data` on entities.
- Map generated DTO types to entity types carefully: Integer -> Long for ids, OffsetDateTime -> Instant for timestamps.

7) Build & validation
- After creating api and service modules:
  - Run `mvn -pl <api-module-path> -am clean install -DskipTests=true` (validate codegen and compile of API).
  - Then run `mvn -pl <service-module-path> -am clean install -DskipTests=true`.
  - Finally run `mvn -DskipTests=true clean install` in repo root as a full sanity check.
- If codegen prints generator warnings, fix YAML per OpenAPI checklist and re-run.

8) Commit & PR rules
- Do NOT commit `.gen` or generated outputs that duplicate `common/api`.
- Do NOT modify `common` in a service-change PR. If `common` must change, open a separate PR against `common` only.
- PR checklist (auto-assert before creating PR):
  - Ran api codegen + build and no generator warnings remain.
  - No plugin re-declarations in child POMs.
  - No `.gen` files are committed.
  - All security checks use `RequireRoles`/SecurityContext; no `X-User` usage remains.

9) If anything in `common` is required (types/annotations), do not invent local copies — request a `common` change and wait for that artifact to be updated and re-built.

Service skeleton (required files — copy/paste template)
- <service>-ms/
  - <service>-api/
    - src/main/resources/<service>-api.yaml
    - pom.xml (no plugin versions; depends on com.corems.common:api)
  - <service>-service/
    - src/main/java/... (implementation)
    - src/main/resources/application.yaml
    - src/main/resources/db-config.yaml
    - src/main/resources/security-config.yaml
    - .env-example
    - README.md

Role naming & env guidance (short)
- Role name convention: use `<SERVICE_UPPER>_MS_ADMIN` for admin role (e.g., TRANSLATION_MS_ADMIN). The LLM MUST map "admin" in the prompt to that enum value in `com.corems.common.security.CoreMsRoles`.
- `.env-example` must only list variable NAMES (no secrets). Only include service-specific optional vars that the service actually needs (e.g., `ALLOWED_DOMAINS`, `CACHE_TTL_SECONDS`) — do not copy auth-service OAuth keys into other services.

CI / pre-commit (recommended; short)
- Add a git pre-commit or CI check that fails on accidentally committed `.gen` files and that runs `mvn -pl <changed-api> -am -DskipTests=true validate` for changed api modules.

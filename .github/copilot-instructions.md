# 🧠 Copilot Instructions for Core Microservices (Backend)

These are project-specific guidelines for GitHub Copilot and other AI assistants working on the **Core Microservices backend**.  
The goal is to keep contributions **consistent, secure, and aligned** with the project vision.

---

## ⚙️ Project Overview
**Core Microservices** is a modular backend platform providing a foundation for modern startups.  
It includes common microservices like:
- **User Management (User-MS)**: Authentication, authorization, and user profile management.
- **Notification Service (communication-MS)**: Email, SMS, and in-app messaging.
- **Document Management (Document-MS)**: Upload, store, and share documents securely.
- More services to come (Template, Questionnaire, Shop).

The project uses:
- **Java 17**  
- **Spring Boot 3.3+**  
- **Spring Security (JWT + OAuth2)**  
- **Spring Cloud**  
- **Apache Kafka (event-driven)**  
- **Flyway** (database migrations)  
- **Spring Boot Actuator** (observability)  
- **Maven (multi-module structure)**

---

## 🧩 Project Structure

```
corems-backend/
├── parent/ (root pom.xml)
│
├── common/            → Shared libraries, utils, DTOs, base config
├── user-ms/           → User Management service
│   ├── user-api/      → OpenAPI interfaces, DTOs, contracts
│   ├── user-service/  → Business logic, controllers, repositories
    .env - file for environment variables
│
├── communication-ms/   → Notification handling (email, Kafka events)
└── document-ms/       → File and metadata management
```

---

## 🧠 Coding Guidelines for Copilot

### ✅ General Rules
1. **Prefer clean, minimal, and modular code.**
2. Use **Java 17 features** (records, pattern matching, switch expressions) where appropriate.
3. Always follow **SOLID** principles.
4. Avoid adding unnecessary annotations or frameworks.

---

### 🔐 Security
- Use **Spring Security with JWT** for REST endpoints.
- OAuth2 is already configured — **do not override it** unless adding a new provider.
- Never log sensitive information (passwords, tokens, user data).
- When generating tokens, always include `sub`, `email`, and `roles`.

---

### 💬 API Design
- APIs must be defined in the `*-api` module using **OpenAPI 3.0**.
- Use consistent REST patterns:
  - `GET /api/resource` — list or retrieve
  - `POST /api/resource` — create
  - `PUT /api/resource/{id}` — update
  - `DELETE /api/resource/{id}` — delete
- Error responses must use the standard `ErrorWrapper` schema.

---

### 🗄️ Database and Flyway
- Database schema changes must be versioned via **Flyway migrations** under:
  ```
  src/main/resources/db/migration/
  ```.
- Follow snake_case naming for database columns.


---

### 🧩 Common Module
- Shared code (utilities, exception handling, constants, Kafka config) goes in the **`common`** module.
- Avoid business logic inside `common`.
- Use `common` for cross-cutting concerns only (e.g., logging, error handling, security).

---

### 🧱 Service Modules
Each service should include:
- `Controller` layer — handles API requests
- `Service` layer — business logic
- `Repository` layer — database interaction
- `Config` — for Spring beans, Kafka, and Security

---

### 🧪 Testing
- Use **JUnit 5 + Mockito** for unit tests.
- Use **Testcontainers** for integration tests (PostgreSQL, Kafka).
- Follow the test naming pattern:
  ```
  {ClassName}Test.java
  ```
- Minimum coverage: **80% for core logic**.

---

### 📦 Build & Run
To build the full backend:
```bash
mvn clean install
```

To run a specific service:
```bash
cd user-ms/user-service
mvn spring-boot:run
```

---

### 🌍 Environment Configuration
Each service must use:
- `application.yml` with main properties, and include other YAML files as needed for different needs such as: security, db, queue, log etc.
- variables distributed by these files and in future can be moved into common package: like observability, tracing, logging etc.

---

## ⚠️ Do NOT
- Do not use Lombok’s `@Data` (use `@Getter`/`@Setter`/`@Builder` instead).
- Do not expose entities directly via API.
- Do not add new dependencies without approval (maintain lightweight footprint).
- 
---

## 🔎 SearchableRepository & Generic Search/Filter Pattern (db-utils)

To keep search/filter/sort behavior consistent across services we use a small, reusable pattern implemented in the `common` packages (module: `common/utils/db-utils`). Additions and expectations for Copilot/PRs:

- Purpose: make repositories the single source of truth for which fields are searchable, filterable or sortable, and provide optional API -> JPA field aliases.
- Key contract: repository interfaces may implement `SearchableRepository<T, ID>` (a `@NoRepositoryBean` extension of Spring Data) which exposes defaults that services can override.

Core methods on the contract (defaults return empty lists/maps):
- `List<String> getSearchFields()` — API-friendly names to search with (free-text search across these fields).
- `List<String> getAllowedFilterFields()` — whitelist of API-visible fields allowed in filter requests.
- `List<String> getAllowedSortFields()` — whitelist of fields allowed for sorting.
- `Map<String,String> getFieldAliases()` — optional mapping of API field -> JPA attribute path (dot-separated) for nested attributes.

Design notes and rules for Copilot:
- Do not put per-entity searchable/filterable field lists in YAML or environment files — put them on the repository interface itself (for example `UserRepository` overrides defaults). This keeps metadata typed and co-located with the repository.
- `QueryParams` (the request carrier) must only contain client-supplied values: page, pageSize, search, sort, filters. It must NOT carry repository metadata.
- Pagination is 1-based at the API boundary. `PaginationUtil` converts incoming 1-based page to Spring's 0-based index internally. The `PaginatedQueryExecutor` returns pages adapted to present 1-based page numbers to callers (via a small `PageOneBased` adapter). Service code should not add +1 to page numbers.

How the executor uses the repository metadata (high-level):
1. Discoverable metadata: when a repository implements `SearchableRepository` the executor reads `getSearchFields()`, `getAllowedFilterFields()`, `getAllowedSortFields()` and `getFieldAliases()`.
2. Aliases are resolved first: incoming API field names are mapped via `getFieldAliases()` to JPA paths before validation and spec construction.
3. Validation: requested filter fields are validated against the allowed list (the validator accepts either API names or resolved JPA paths).
4. Specification building: the executor uses a small Specification builder to convert resolved filters and the free-text search into a `Specification<T>` and executes it via `JpaSpecificationExecutor`.
5. Pagination & sorting: `PaginationUtil.buildPageable(...)` accepts the (1-based) page and the repository's allowed sort fields to produce a `Pageable`.

Alias rules and examples
- Alias map keys are API field names (the fields you expose in DTOs / query params). Values are JPA attribute paths, e.g. `address.city`, `provider.name`.
- If an alias is missing for an API field, the executor treats the API field as the JPA path.
- Example mapping: `Map.of("city", "address.city", "provider", "provider.name")`.

Runtime validation recommendation
- Always do existence and type checks before building Criteria/Specifications:
  - Existence: ensure the resolved JPA path exists on the entity (use the JPA Metamodel or reflection on the entity class).
  - Type checks: convert and validate filter values against the attribute Java type (numbers, booleans, dates) to avoid Criteria API runtime errors.
- We ship a small helper `EntityFieldValidator` in `db-utils`. Use it before creating the Specification. If the validator cannot resolve or convert a value, return a client-friendly 4xx (IllegalArgumentException → map to BadRequest) rather than letting Criteria throw a 500.

Testing guidance
- Add a lightweight integration test per module verifying:
  - free-text search using repository-provided `getSearchFields()`;
  - filtering by allowed fields returns expected results;
  - unknown field filter produces a validation error (4xx/IllegalArgumentException in unit tests);
  - sort whitelist respected by `PaginationUtil`.
- Tests should use in-memory H2 (or Testcontainers Postgres) and `@DataJpaTest` to exercise Spring Data repository behaviour.

Migration notes
- If you previously passed search/sort/filter allowed lists in `QueryParams` or YAML, move those lists into the corresponding repository interface as default methods or constants and remove them from the request objects.
- Update service code to call `PaginatedQueryExecutor.execute(repo, repo, params)` (both repo and spec executor) and map results to API DTOs using a small factory/helper. Do not add manual +1 adjustments to pages — the executor returns 1-based numbering.

Minimal repository example (pattern)
- A `UserRepository` may look like:

```java
public interface UserRepository extends SearchableRepository<User, Long> {
    @Override
    default List<String> getSearchFields() {
        return List.of("email", "firstName", "lastName");
    }

    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("email", "provider", "createdAt");
    }

    @Override
    default Map<String,String> getFieldAliases() {
        return Map.of("createdAt","createdAt");
    }
}
```

When to prefer Querydsl (optional)
- Querydsl gives stronger compile-time checks and an expressive DSL. We chose a lightweight Specification-based solution for portability and to avoid additional compile and dependency overhead. If a team requires complex dynamic joins and predicates frequently, introducing Querydsl as a separate module is reasonable — but please open a proposal with cost/benefit and migration steps.

Style & PR guidance
- Keep the executor small and unit-testable. Prefer to extract small helpers (AliasResolver, FilterValidator, SearchSpecFactory) rather than growing a single large method.
- Document repository metadata in the repository interface (small Javadoc) so other devs can see what fields are supported.
- Add integration tests for any change that touches the executor or repository metadata.

---

## 📚 References
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security JWT Guide](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Flyway Docs](https://flywaydb.org/documentation/)
- [Spring Data JPA Specifications](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications)
- [Spring Boot Testing Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/testing.html)

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


---

## 📚 References
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security JWT Guide](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Flyway Docs](https://flywaydb.org/documentation/)

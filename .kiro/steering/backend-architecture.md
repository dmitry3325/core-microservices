---
inclusion: always
---

# Backend Architecture - Core Microservices

## Platform
- Java 25, Spring Boot 4
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

**Formatting**:
- Use Javadoc for public APIs (required for controller/service methods)
- Use `//` for short rationale only
- Tag actionable items: `TODO:` / `FIXME:` with owner/ticket

## PR Checklist
- ✅ Avoided editing `common`
- ✅ Used generated models instead of local DTOs
- ✅ Ran API codegen + build for `*-api`
- ✅ Avoided re-declaring parent-managed plugins
- ✅ Did not commit `.gen` files

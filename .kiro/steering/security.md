---
inclusion: fileMatch
fileMatchPattern: "**/{security,auth,controller,service}/**/*.java"
---

# Security Guidelines

## Roles
Use roles from `CoreMsRoles` enum in `com.corems.common.security`:

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

## Identity Resolution
- Use `com.corems.common.security.SecurityUtils` helper:
  - `SecurityUtils.getUserPrincipal()` - returns UserPrincipal or throws UNAUTHORIZED
  - `SecurityUtils.getUserPrincipalOptional()` - returns Optional<UserPrincipal>
- Resolve identity from Spring Security only
- Cast to `UserPrincipal` in service layer
- DO NOT rely on X-User or client-supplied headers

## Role-Based Access
```java
@RequireRoles(CoreMsRoles.USER_MS_ADMIN)
public ResponseEntity<SuccessfulResponse> adminOnlyEndpoint() { }
```

## Sender Identity (Messages/Notifications)
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

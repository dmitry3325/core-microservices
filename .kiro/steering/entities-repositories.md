---
inclusion: fileMatch
fileMatchPattern: "**/{entity,repository}/**/*.java"
---

# Entities & Repositories

## Entity Annotations
```java
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @EqualsAndHashCode.Include
    private UUID id;
    // ...
}
```

### Lombok Rules
- Use: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Use: `@EqualsAndHashCode(onlyExplicitlyIncluded=true)` with id included
- Avoid `@Data` on JPA entities (causes issues with lazy loading)

## Repository Pattern
Repositories MUST implement `SearchableRepository<T,ID>`:

```java
public interface UserRepository extends JpaRepository<User, UUID>, SearchableRepository<User, UUID> {
    
    @Override
    default Set<String> getSearchFields() {
        return Set.of("email", "firstName", "lastName");
    }
    
    @Override
    default Set<String> getAllowedFilterFields() {
        return Set.of("provider", "status", "createdAt");
    }
    
    @Override
    default Set<String> getAllowedSortFields() {
        return Set.of("createdAt", "email", "firstName");
    }
    
    @Override
    default Map<String, String> getFieldAliases() {
        return Map.of();
    }
}
```

## DTO Mapping
Map DTOs carefully between generated models and entities:
- `Integer` (API) -> `Long` (Entity) for IDs
- `OffsetDateTime` (API) -> `Instant` (Entity) for timestamps

## Paginated Queries
Use `PaginatedQueryExecutor.execute(...)` for listing endpoints:

```java
UsersPagedResponse response = PaginatedQueryExecutor.execute(
    userRepository,
    page, pageSize, sort, search, filter,
    this::mapToUserInfo
);
```

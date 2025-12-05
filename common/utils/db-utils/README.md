# DB Utils - SearchableRepository

A powerful, metadata-driven repository pattern for Spring Data JPA that enables declarative search, filter, and sort capabilities with minimal code.

## Table of Contents
- [Overview](#overview)
- [Quick Start](#quick-start)
- [Features](#features)
- [API Reference](#api-reference)
  - [Search](#search)
  - [Filter](#filter)
  - [Sort](#sort)
  - [Pagination](#pagination)
  - [Collection Fields](#collection-fields)
  - [Field Aliases](#field-aliases)
- [Complete Examples](#complete-examples)
- [Advanced Usage](#advanced-usage)

---

## Overview

`SearchableRepository` extends Spring Data JPA repositories with powerful query capabilities:
- ✅ Free-text search across multiple fields
- ✅ Type-safe filtering with multiple operators
- ✅ Multi-field sorting
- ✅ Automatic pagination
- ✅ Collection field support (JOIN)
- ✅ Field aliasing for API compatibility
- ✅ Zero boilerplate query code

## Quick Start

### 1. Create your entity

```java
@Entity
@Table(name = "products")
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    private BigDecimal price;
    
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    
    @ElementCollection
    private Set<String> tags;
    
    private Instant createdAt;
    private Boolean featured;
}
```

### 2. Create repository with metadata

```java
@Repository
public interface ProductRepository extends SearchableRepository<ProductEntity, Long> {
    
    @Override
    default List<String> getSearchFields() {
        return List.of("name", "description", "tags");
    }
    
    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("status", "price", "featured", "createdAt", "tags");
    }
    
    @Override
    default List<String> getAllowedSortFields() {
        return List.of("name", "price", "createdAt", "featured");
    }
    
    @Override
    default List<String> getCollectionFields() {
        return List.of("tags");  // tags is @ElementCollection
    }
    
    @Override
    default Map<String, String> getFieldAliases() {
        return Map.of(
            "created_at", "createdAt",
            "is_featured", "featured"
        );
    }
}
```

### 3. Use in service layer

```java
@Service
public class ProductService {
    private final ProductRepository repository;
    
    public Page<ProductEntity> searchProducts(
            Optional<String> search,
            Optional<String> sort,
            Optional<List<String>> filters,
            Optional<Integer> page,
            Optional<Integer> pageSize) {
        
        QueryParams params = new QueryParams(page, pageSize, search, sort, filters);
        return repository.findAllByQueryParams(params);
    }
}
```

### 4. Call from controller

```http
GET /api/products?search=laptop&filter=status:ACTIVE,price:lt:1000&sort=price:asc&page=1&pageSize=20
```

---

## Features

### Search
Free-text search across multiple fields with LIKE queries (case-insensitive).

```java
// Repository
default List<String> getSearchFields() {
    return List.of("name", "description", "sku");
}

// Query
?search=wireless keyboard

// Generated SQL
WHERE (name LIKE '%wireless keyboard%' 
   OR description LIKE '%wireless keyboard%' 
   OR sku LIKE '%wireless keyboard%')
```

### Filter
Type-safe filtering with multiple operators.

**Supported operators:**
- `eq` - equals (default)
- `ne` - not equals
- `gt` - greater than
- `gte` - greater than or equal
- `lt` - less than
- `lte` - less than or equal
- `like` - pattern matching
- `contains` - for comma-separated values
- `in` - value in list

```java
// Repository
default List<String> getAllowedFilterFields() {
    return List.of("status", "price", "category", "inStock");
}

// Query examples
?filter=status:eq:ACTIVE                    // status = 'ACTIVE'
?filter=status:ACTIVE                       // same (eq is default)
?filter=price:gte:100                       // price >= 100
?filter=price:gte:100,price:lte:500         // price BETWEEN 100 AND 500
?filter=category:in:Electronics,Computers   // category IN ('Electronics', 'Computers')
?filter=inStock:eq:true                     // inStock = true
```

**Type conversion:**
The framework automatically converts string values to the correct Java type:
- `String` → as-is
- `Integer/Long/Double/Float` → parsed
- `Boolean` → `true`/`false`
- `Instant/OffsetDateTime` → ISO-8601 format
- `UUID` → validated UUID string
- `Enum` → enum value name

### Sort
Multi-field sorting with direction control.

```java
// Repository
default List<String> getAllowedSortFields() {
    return List.of("name", "price", "createdAt", "rating");
}

// Query examples
?sort=price:asc                  // ORDER BY price ASC
?sort=createdAt:desc             // ORDER BY createdAt DESC
?sort=price:desc,name:asc        // ORDER BY price DESC, name ASC
?sort=rating                     // ORDER BY rating DESC (default)
```

### Pagination
Standard page-based pagination (1-indexed).

```java
?page=1&pageSize=20              // First page, 20 items
?page=2&pageSize=50              // Second page, 50 items

// Response includes:
{
  "items": [...],
  "page": 1,
  "pageSize": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

**Defaults:**
- Default page: `1`
- Default page size: `20`
- Max page size: `1000`

### Collection Fields
 Automatic JOIN support for JPA collection fields.

```java
@Entity
public class DocumentEntity {
    // Regular fields
    private String name;
    
    // Collection field (requires JOIN)
    @ElementCollection
    private Set<String> tags;
}

@Repository
public interface DocumentRepository extends SearchableRepository<DocumentEntity, Long> {
    
    @Override
    default List<String> getSearchFields() {
        return List.of("name", "tags");  // tags is a collection
    }
    
    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("name", "tags");  // tags can be filtered
    }
    
    @Override
    default List<String> getCollectionFields() {
        return List.of("tags");  // ⚠️ REQUIRED: declare collection fields
    }
}

// Usage
?search=important                     // searches in name AND tags (with JOIN)
?filter=tags:contains:finance         // filters by tags (with JOIN)
```

**Important:** Always declare collection fields in `getCollectionFields()`, otherwise you'll get runtime exceptions.

### Field Aliases
Provide API-friendly names for entity fields.

```java
@Override
default Map<String, String> getFieldAliases() {
    return Map.of(
        "created_at", "createdAt",       // snake_case → camelCase
        "updated_at", "updatedAt",
        "owner_id", "userId",            // semantic alias
        "is_active", "active"
    );
}

// API usage (with alias)
?filter=created_at:gt:2024-01-01
?sort=created_at:desc

// Internally resolved to
WHERE createdAt > '2024-01-01'
ORDER BY createdAt DESC
```

---

## API Reference

### QueryParams

```java
public record QueryParams(
    Optional<Integer> page,           // Page number (1-based)
    Optional<Integer> pageSize,       // Items per page
    Optional<String> search,          // Free-text search
    Optional<String> sort,            // Sort specification
    Optional<List<String>> filters    // Filter list
) {}
```

### SearchableRepository Methods

| Method | Purpose | Return Type |
|--------|---------|-------------|
| `getSearchFields()` | Fields for free-text search | `List<String>` |
| `getAllowedFilterFields()` | Fields that can be filtered | `List<String>` |
| `getAllowedSortFields()` | Fields that can be sorted | `List<String>` |
| `getFieldAliases()` | API name → Entity field mapping | `Map<String, String>` |
| `getCollectionFields()` | Collection fields requiring JOIN | `List<String>` |
| `findAllByQueryParams(params)` | Execute query | `Page<T>` |

---

## Complete Examples

### Example 1: E-commerce Product Repository

```java
@Entity
@Table(name = "products")
public class ProductEntity {
    @Id
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    
    @Enumerated(EnumType.STRING)
    private ProductStatus status;  // ACTIVE, DISCONTINUED, OUT_OF_STOCK
    
    @ElementCollection
    private Set<String> categories;
    
    @ElementCollection
    private Set<String> tags;
    
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean featured;
}

@Repository
public interface ProductRepository extends SearchableRepository<ProductEntity, Long> {
    
    @Override
    default List<String> getSearchFields() {
        // Search in name, description, and tags
        return List.of("name", "description", "tags");
    }
    
    @Override
    default List<String> getAllowedFilterFields() {
        return List.of(
            "status",           // enum
            "price",            // numeric
            "stock",            // numeric
            "featured",         // boolean
            "categories",       // collection
            "tags",             // collection
            "createdAt",        // timestamp
            "updatedAt"         // timestamp
        );
    }
    
    @Override
    default List<String> getAllowedSortFields() {
        return List.of("name", "price", "createdAt", "stock", "featured");
    }
    
    @Override
    default List<String> getCollectionFields() {
        // Declare all collection fields
        return List.of("categories", "tags");
    }
    
    @Override
    default Map<String, String> getFieldAliases() {
        return Map.of(
            "created_at", "createdAt",
            "updated_at", "updatedAt",
            "in_stock", "stock"
        );
    }
}
```

**Usage Examples:**

```http
# Search for "laptop" in name, description, and tags
GET /api/products?search=laptop

# Active products under $1000
GET /api/products?filter=status:ACTIVE,price:lt:1000

# Featured products in Electronics category, sorted by price
GET /api/products?filter=featured:true,categories:contains:Electronics&sort=price:asc

# Products with specific tag, out of stock
GET /api/products?filter=tags:contains:clearance,stock:eq:0

# Complex query: search + multiple filters + sort + pagination
GET /api/products?search=wireless&filter=status:ACTIVE,price:gte:50,price:lte:200,featured:true&sort=price:asc,name:asc&page=1&pageSize=25

# Using aliases (created_at instead of createdAt)
GET /api/products?filter=created_at:gt:2024-01-01&sort=created_at:desc
```

### Example 2: User Repository

```java
@Entity
public class UserEntity {
    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    private Boolean isActive;
    private Instant createdAt;
    private Instant lastLoginAt;
}

@Repository
public interface UserRepository extends SearchableRepository<UserEntity, Long> {
    
    @Override
    default List<String> getSearchFields() {
        return List.of("firstName", "lastName", "email");
    }
    
    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("role", "isActive", "createdAt", "lastLoginAt");
    }
    
    @Override
    default List<String> getAllowedSortFields() {
        return List.of("firstName", "lastName", "email", "createdAt", "lastLoginAt");
    }
    
    @Override
    default Map<String, String> getFieldAliases() {
        return Map.of(
            "name", "firstName",        // convenience alias
            "active", "isActive",
            "created", "createdAt",
            "last_login", "lastLoginAt"
        );
    }
}
```

**Usage Examples:**

```http
# Search users by name or email
GET /api/users?search=john.doe@example.com

# Active admin users
GET /api/users?filter=role:ADMIN,active:true

# Users who logged in recently
GET /api/users?filter=last_login:gt:2024-11-01T00:00:00Z&sort=last_login:desc

# Inactive users created this year
GET /api/users?filter=active:false,created:gte:2024-01-01T00:00:00Z
```

### Example 3: Document Repository (with Collections)

```java
@Entity
public class DocumentEntity {
    @Id
    private Long id;
    private String name;
    private String description;
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    private Visibility visibility;
    
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> tags;
    
    private String extension;
    private Long size;
    private Instant createdAt;
    private Boolean deleted;
}

@Repository
public interface DocumentRepository extends SearchableRepository<DocumentEntity, Long> {
    
    @Override
    default List<String> getSearchFields() {
        return List.of("name", "description", "tags");
    }
    
    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("userId", "visibility", "extension", "tags", "deleted", "createdAt");
    }
    
    @Override
    default List<String> getAllowedSortFields() {
        return List.of("name", "size", "createdAt");
    }
    
    @Override
    default List<String> getCollectionFields() {
        return List.of("tags");  // ⚠️ Required for @ElementCollection
    }
    
    @Override
    default Map<String, String> getFieldAliases() {
        return Map.of(
            "owner", "userId",
            "created_at", "createdAt",
            "file_name", "name"
        );
    }
}
```

**Usage Examples:**

```http
# Search documents by tag
GET /api/documents?search=report

# User's non-deleted documents
GET /api/documents?filter=owner:e76c3c20-fa65-49a9-8ea3-fc0073730547,deleted:false

# Public PDF documents
GET /api/documents?filter=visibility:PUBLIC,extension:pdf

# Documents with specific tag
GET /api/documents?filter=tags:contains:financial-2024

# Recent large documents
GET /api/documents?filter=size:gt:1000000,created_at:gt:2024-12-01&sort=created_at:desc
```

### Example 4: Product with Categories (Nested Collections)

```java
@Entity
public class ProductEntity {
    @Id
    private Long id;
    private String name;
    private String sku;
    private BigDecimal price;
    
    @ManyToMany
    @JoinTable(
        name = "product_category",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<CategoryEntity> categories;
    
    @ElementCollection
    private Set<String> tags;
    
    private Instant createdAt;
}

@Entity
public class CategoryEntity {
    @Id
    private Long id;
    private String name;
    private String code;
}

@Repository
public interface ProductRepository extends SearchableRepository<ProductEntity, Long> {
    
    @Override
    default List<String> getSearchFields() {
        return List.of(
            "name",             // product name
            "sku",              // product SKU
            "categories.name",  // category name (nested)
            "tags"              // product tags
        );
    }
    
    @Override
    default List<String> getAllowedFilterFields() {
        return List.of(
            "price",
            "categories.name",  // filter by category name
            "categories.code",  // filter by category code
            "categories.id",    // filter by category ID
            "tags",
            "createdAt"
        );
    }
    
    @Override
    default List<String> getAllowedSortFields() {
        return List.of("name", "price", "createdAt");
    }
    
    @Override
    default List<String> getCollectionFields() {
        // Declare only the base collection fields
        return List.of("categories", "tags");
    }
    
    @Override
    default Map<String, String> getFieldAliases() {
        return Map.of(
            "category", "categories.name",
            "created_at", "createdAt"
        );
    }
}
```

**Usage Examples:**

```http
# Search across product name, SKU, category names, and tags
GET /api/products?search=electronics

# Products in specific category
GET /api/products?filter=categories.name:eq:Electronics

# Products in category with code
GET /api/products?filter=categories.code:eq:ELEC

# Filter by multiple nested fields
GET /api/products?filter=categories.name:like:Tech,price:lt:500

# Using alias (category instead of categories.name)
GET /api/products?filter=category:eq:Computers

# Complex query with nested collection search and filter
GET /api/products?search=wireless&filter=categories.name:eq:Electronics,price:gte:50,tags:contains:sale&sort=price:asc
```

---

## Advanced Usage

### Service Layer Integration

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repository;
    
    public Page<ProductEntity> searchProducts(
            Optional<String> search,
            Optional<String> sort,
            Optional<List<String>> filters,
            Optional<Integer> page,
            Optional<Integer> pageSize) {
        
        // Add business logic filters
        List<String> allFilters = new ArrayList<>(filters.orElse(List.of()));
        allFilters.add("deleted:false");  // Always exclude deleted
        
        // Build and execute query
        QueryParams params = new QueryParams(
            page, 
            pageSize, 
            search, 
            sort, 
            Optional.of(allFilters)
        );
        
        return repository.findAllByQueryParams(params);
    }
    
    public Page<ProductEntity> getFeaturedProducts(int page, int pageSize) {
        QueryParams params = new QueryParams(
            Optional.of(page),
            Optional.of(pageSize),
            Optional.empty(),
            Optional.of("createdAt:desc"),
            Optional.of(List.of("featured:true", "status:ACTIVE"))
        );
        
        return repository.findAllByQueryParams(params);
    }
}
```

### Controller Layer

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    
    @GetMapping
    public ResponseEntity<Page<ProductDTO>> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) List<String> filter,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        
        Page<ProductEntity> results = productService.searchProducts(
            Optional.ofNullable(search),
            Optional.ofNullable(sort),
            Optional.ofNullable(filter),
            Optional.of(page),
            Optional.of(pageSize)
        );
        
        Page<ProductDTO> dtos = results.map(this::toDTO);
        return ResponseEntity.ok(dtos);
    }
}
```

### Error Handling

The framework throws `ServiceException` for invalid queries:

```java
// Invalid filter field (not in getAllowedFilterFields)
?filter=password:eq:secret
// → ServiceException: "Invalid filter field: password"

// Invalid enum value
?filter=status:eq:INVALID_STATUS
// → ServiceException: "Invalid enum value 'INVALID_STATUS' for field 'status'"

// Invalid date format
?filter=createdAt:gt:invalid-date
// → ServiceException: "Invalid instant value 'invalid-date' for field 'createdAt' (expected ISO-8601)"

// Invalid UUID
?filter=userId:eq:not-a-uuid
// → ServiceException: "Invalid UUID value 'not-a-uuid' for field 'userId'"
```

### Testing

```java
@DataJpaTest
class ProductRepositoryTest {
    
    @Autowired
    private ProductRepository repository;
    
    @Test
    void searchByName() {
        QueryParams params = new QueryParams(
            Optional.of(1),
            Optional.of(10),
            Optional.of("laptop"),
            Optional.empty(),
            Optional.empty()
        );
        
        Page<ProductEntity> results = repository.findAllByQueryParams(params);
        
        assertThat(results.getContent()).isNotEmpty();
        assertThat(results.getContent())
            .allMatch(p -> p.getName().toLowerCase().contains("laptop"));
    }
    
    @Test
    void filterByPriceRange() {
        QueryParams params = new QueryParams(
            Optional.of(1),
            Optional.of(10),
            Optional.empty(),
            Optional.empty(),
            Optional.of(List.of("price:gte:100", "price:lte:500"))
        );
        
        Page<ProductEntity> results = repository.findAllByQueryParams(params);
        
        assertThat(results.getContent())
            .allMatch(p -> p.getPrice().compareTo(new BigDecimal("100")) >= 0
                        && p.getPrice().compareTo(new BigDecimal("500")) <= 0);
    }
}
```

---

## Best Practices

1. **Security**: Never expose all fields for filtering. Only include safe, non-sensitive fields in `getAllowedFilterFields()`.

2. **Performance**: Add database indexes for fields in `getAllowedFilterFields()` and `getAllowedSortFields()`. For nested collections, index the foreign key columns.

3. **Collection Fields**: Always declare `@ElementCollection`, `@OneToMany`, `@ManyToMany` base fields in `getCollectionFields()`. Use dot notation for nested paths (e.g., "categories.name").

4. **Validation**: Let the framework handle type conversion and validation. Don't pre-validate query parameters.

5. **Pagination**: Set reasonable default and max page sizes to prevent performance issues.

6. **Aliases**: Use aliases for backwards compatibility, not as a primary API design strategy. Aliases can map to nested paths (e.g., "category" → "categories.name").

7. **Nested Paths**: When using nested collection paths, ensure the base collection is properly indexed and consider the JOIN performance impact.

---

## Troubleshooting

### "Index out of bounds" exception
**Cause**: Malformed sort parameter (e.g., `sort=:asc` or `sort=field::`)  
**Solution**: Fixed in PaginationUtil - validates and skips malformed parts

### "Invalid filter field" exception
**Cause**: Trying to filter on a field not in `getAllowedFilterFields()`  
**Solution**: Add the field to `getAllowedFilterFields()` or remove it from the query

### Runtime exception with collection fields
**Cause**: Collection field used in search/filter but not declared in `getCollectionFields()`  
**Solution**: Add the field to `getCollectionFields()`

### No results despite matching data
**Cause**: Case-sensitive search or incorrect filter operator  
**Solution**: Search is case-insensitive by default; check filter operator and value type

---

## License

Part of the Core Microservices project.

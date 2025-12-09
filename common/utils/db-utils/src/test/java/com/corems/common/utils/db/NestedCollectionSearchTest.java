package com.corems.common.utils.db;

import com.corems.common.utils.db.entity.TestCategoryEntity;
import com.corems.common.utils.db.entity.TestProductEntity;
import com.corems.common.utils.db.repo.TestProductRepository;
import com.corems.common.utils.db.utils.QueryParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for nested collection path support in SearchableRepository.
 * Tests searching and filtering on nested entity fields like categories.name.
 */
@SpringBootTest(classes = NestedCollectionSearchTest.TestConfig.class)
@Transactional
public class NestedCollectionSearchTest {

    @SpringBootApplication(scanBasePackageClasses = {TestProductEntity.class, TestCategoryEntity.class})
    @EnableJpaRepositories(basePackageClasses = TestProductRepository.class)
    static class TestConfig {}

    @Autowired
    private TestProductRepository productRepo;

    private TestCategoryEntity electronics;
    private TestCategoryEntity computers;
    private TestCategoryEntity accessories;

    @BeforeEach
    void setUp() {
        productRepo.deleteAll();

        // Create categories
        electronics = new TestCategoryEntity("Electronics", "ELEC");
        computers = new TestCategoryEntity("Computers", "COMP");
        accessories = new TestCategoryEntity("Accessories", "ACC");

        // Create products with categories and tags
        TestProductEntity laptop = new TestProductEntity("Laptop Pro", "LP-001", new BigDecimal("1299.99"));
        laptop.addCategory(electronics);
        laptop.addCategory(computers);
        laptop.addTag("business");
        laptop.addTag("portable");

        TestProductEntity mouse = new TestProductEntity("Wireless Mouse", "WM-001", new BigDecimal("29.99"));
        mouse.addCategory(electronics);
        mouse.addCategory(accessories);
        mouse.addTag("wireless");
        mouse.addTag("portable");

        TestProductEntity keyboard = new TestProductEntity("Mechanical Keyboard", "MK-001", new BigDecimal("149.99"));
        keyboard.addCategory(electronics);
        keyboard.addCategory(accessories);
        keyboard.addTag("mechanical");
        keyboard.addTag("gaming");

        TestProductEntity desktop = new TestProductEntity("Desktop PC", "DT-001", new BigDecimal("1999.99"));
        desktop.addCategory(computers);
        desktop.addTag("business");
        desktop.addTag("powerful");

        productRepo.save(laptop);
        productRepo.save(mouse);
        productRepo.save(keyboard);
        productRepo.save(desktop);
    }

    @Test
    void searchByNestedCategoryName() {
        // Search for "Computers" should find products in Computers category
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.of("Computers"),
                Optional.empty(),
                Optional.empty()
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        assertThat(page.getTotalElements()).isEqualTo(2); // Laptop and Desktop
        assertThat(page.getContent())
                .extracting(TestProductEntity::getName)
                .containsExactlyInAnyOrder("Laptop Pro", "Desktop PC");
    }

    @Test
    void searchByNestedCategoryNamePartial() {
        // Search for "elect" should match "Electronics" category
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.of("elect"),
                Optional.empty(),
                Optional.empty()
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        // Should find all products in Electronics category (3 products)
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void filterByNestedCategoryNameEquals() {
        // Filter by exact category name
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.of(List.of("categories.name:eq:Accessories"))
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        assertThat(page.getTotalElements()).isEqualTo(2); // Mouse and Keyboard
        assertThat(page.getContent())
                .extracting(TestProductEntity::getName)
                .containsExactlyInAnyOrder("Wireless Mouse", "Mechanical Keyboard");
    }

    @Test
    void filterByNestedCategoryCode() {
        // Filter by category code
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.of(List.of("categories.code:eq:COMP"))
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        assertThat(page.getTotalElements()).isEqualTo(2); // Laptop and Desktop
        assertThat(page.getContent())
                .extracting(TestProductEntity::getName)
                .containsExactlyInAnyOrder("Laptop Pro", "Desktop PC");
    }

    @Test
    void filterByNestedCategoryNameWithLike() {
        // Filter using LIKE on category name
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.of(List.of("categories.name:like:Comp"))
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        assertThat(page.getTotalElements()).isEqualTo(2); // Laptop and Desktop
    }

    @Test
    void filterByMultipleNestedFields() {
        // Filter by category name AND price
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.of(List.of(
                        "categories.name:eq:Electronics",
                        "price:lt:100"
                ))
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        assertThat(page.getTotalElements()).isEqualTo(1); // Only Mouse (29.99)
        assertThat(page.getContent().get(0).getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void filterByNestedCategoryAndSimpleCollection() {
        // Filter by category AND tag
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.of(List.of(
                        "categories.name:eq:Computers",
                        "tags:contains:business"
                ))
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        assertThat(page.getTotalElements()).isEqualTo(2); // Laptop and Desktop
    }

    @Test
    void filterByAlias() {
        // Use alias "category" which maps to "categories.name"
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.of(List.of("category:eq:Accessories"))
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        assertThat(page.getTotalElements()).isEqualTo(2); // Mouse and Keyboard
    }

    @Test
    void searchAcrossRegularAndNestedFields() {
        // Search should work across name, sku, categories.name, and tags
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.of("wireless"),
                Optional.empty(),
                Optional.empty()
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        // Should find: Wireless Mouse (by name) and any product with "wireless" tag
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent())
                .anyMatch(p -> p.getName().contains("Wireless"));
    }

    @Test
    void combinedSearchAndNestedFilter() {
        // Search for "Pro" and filter by category
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.of("Pro"),
                Optional.empty(),
                Optional.of(List.of("categories.name:eq:Computers"))
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        assertThat(page.getTotalElements()).isEqualTo(1); // Laptop Pro
        assertThat(page.getContent().get(0).getName()).isEqualTo("Laptop Pro");
    }

    @Test
    void filterByNestedCategoryIn() {
        // Filter by category name IN list
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.of(List.of("categories.name:in:Computers,Accessories"))
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        // All products belong to either Computers or Accessories
        assertThat(page.getTotalElements()).isEqualTo(4);
    }

    @Test
    void sortByNameWithNestedFilter() {
        // Filter by category and sort by name
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.empty(),
                Optional.of("name:asc"),
                Optional.of(List.of("categories.name:eq:Electronics"))
        );

        Page<TestProductEntity> page = productRepo.findAllByQueryParams(params);

        assertThat(page.getTotalElements()).isEqualTo(3);
        // Check that results are sorted by name ascending
        List<String> names = page.getContent().stream()
                .map(TestProductEntity::getName)
                .toList();
        assertThat(names).isSorted();
    }

    @Test
    void paginationWithNestedFilter() {
        // Test pagination works with nested filters
        QueryParams params1 = new QueryParams(
                Optional.of(1),
                Optional.of(2),
                Optional.empty(),
                Optional.of("name:asc"),
                Optional.of(List.of("categories.name:eq:Electronics"))
        );

        Page<TestProductEntity> page1 = productRepo.findAllByQueryParams(params1);

        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalPages()).isEqualTo(2);

        // Get second page
        QueryParams params2 = new QueryParams(
                Optional.of(2),
                Optional.of(2),
                Optional.empty(),
                Optional.of("name:asc"),
                Optional.of(List.of("categories.name:eq:Electronics"))
        );

        Page<TestProductEntity> page2 = productRepo.findAllByQueryParams(params2);
        assertThat(page2.getContent()).hasSize(1);
    }
}


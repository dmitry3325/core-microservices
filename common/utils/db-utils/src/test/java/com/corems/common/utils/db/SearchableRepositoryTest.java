package com.corems.common.utils.db;

import com.corems.common.exception.ServiceException;
import com.corems.common.utils.db.entity.TestEntity;
import com.corems.common.utils.db.repo.TestEntityRepository;
import com.corems.common.utils.db.utils.QueryParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@SpringBootTest(classes = SearchableRepositoryTest.TestConfig.class)
@Transactional
class SearchableRepositoryTest {

    @SpringBootApplication(scanBasePackageClasses = TestEntity.class)
    @EnableJpaRepositories(basePackageClasses = TestEntityRepository.class)
    static class TestConfig {}

    @Autowired
    private TestEntityRepository repo;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
        repo.save(new TestEntity("alice@example.com","Alice","Wonder","local", OffsetDateTime.of(2024,1,1,0,0,0,0, ZoneOffset.UTC).toInstant(), 100.5, true));
        repo.save(new TestEntity("bob@example.com","Bob","Builder","oauth", OffsetDateTime.of(2024,6,1,0,0,0,0, ZoneOffset.UTC).toInstant(), 50.0, false));
        repo.save(new TestEntity("carol@example.com","Carol","Singer","local", OffsetDateTime.of(2025,2,1,0,0,0,0, ZoneOffset.UTC).toInstant(), 200.75, true));
    }

    @Test
    void searchByFreeText() {
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.of("alice"),
                Optional.empty(),
                Optional.empty()
         );

         Page<TestEntity> page = repo.findAllByQueryParams(params);
         assertThat(page.getTotalElements()).isEqualTo(1);
         assertThat(page.getContent().get(0).getEmail()).isEqualTo("alice@example.com");
     }

     @Test
     void filterByAllowedField() {
        QueryParams params = new QueryParams(
                 Optional.of(1),
                 Optional.of(10),
                 Optional.empty(),
                 Optional.empty(),
                 Optional.of(List.of("provider:eq:local")));

          Page<TestEntity> page = repo.findAllByQueryParams(params);
          assertThat(page.getTotalElements()).isEqualTo(2);
      }

    @Test
    void filterByCreatedAtRange() {
        // createdAt > 2024-02-01 -> should match carol only
        QueryParams params = new QueryParams(Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty(), Optional.of(List.of("createdAt:gt:2025-01-01T00:00:00Z")));
        Page<TestEntity> page = repo.findAllByQueryParams(params);
        assertThat(page.getTotalElements()).isEqualTo(1);

        // createdAt < 2024-02-01 -> alice only
        params = new QueryParams(Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty(), Optional.of(List.of("createdAt:lt:2024-02-01T00:00:00Z")));
        page = repo.findAllByQueryParams(params);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void filterByBalanceNumeric() {
        // balance >= 100 -> alice and carol
        QueryParams params = new QueryParams(Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty(), Optional.of(List.of("balance:gte:100")));
        Page<TestEntity> page = repo.findAllByQueryParams(params);
        assertThat(page.getTotalElements()).isEqualTo(2);

        // balance < 60 -> bob only
        params = new QueryParams(Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty(), Optional.of(List.of("balance:lt:60")));
        page = repo.findAllByQueryParams(params);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void filterByBooleanField() {
        // isActive true -> alice & carol
        QueryParams params = new QueryParams(Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty(), Optional.of(List.of("isActive:eq:true")));
        Page<TestEntity> page = repo.findAllByQueryParams(params);
        assertThat(page.getTotalElements()).isEqualTo(2);

        // isActive false -> bob
        params = new QueryParams(Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty(), Optional.of(List.of("isActive:eq:false")));
        page = repo.findAllByQueryParams(params);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

     @Test
     void rejectUnknownFilterField() {
        QueryParams params = new QueryParams(
                Optional.of(1),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.of(List.of("password:eq:x")));

         try {
             repo.findAllByQueryParams(params);
         } catch (Exception ex) {
             assertThat(ex).isInstanceOf(ServiceException.class);
             assertThat(ex.getMessage()).contains("Provided value is invalid");
         }
     }
 }

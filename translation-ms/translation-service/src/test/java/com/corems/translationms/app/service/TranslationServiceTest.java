package com.corems.translationms.app.service;

import com.corems.common.security.SecurityUtils;
import com.corems.common.security.UserPrincipal;
import com.corems.translationms.api.model.RealmsPagedResponse;
import com.corems.translationms.api.model.SuccessfulResponse;
import com.corems.translationms.api.model.TranslationAdminView;
import com.corems.translationms.app.entity.TranslationEntity;
import com.corems.translationms.app.repository.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TranslationServiceTest {

    @Autowired
    private TranslationRepository repository;

    @Autowired
    private TranslationService translationService;

    private TranslationEntity testEntity;
    private Map<String, String> testTranslations;
    private UserPrincipal testUser;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        repository.deleteAll();
        
        testTranslations = Map.of(
            "hello", "Hello",
            "goodbye", "Goodbye",
            "welcome", "Welcome"
        );

        testEntity = new TranslationEntity();
        testEntity.setRealm("default");
        testEntity.setLang("en");
        testEntity.setData(testTranslations);
        testEntity.setUpdatedAt(Instant.now());
        testEntity.setUpdatedBy(UUID.randomUUID());

        testUser = new UserPrincipal(
            UUID.randomUUID(),
            "test@example.com",
            "Test",
            "User",
            UUID.randomUUID(),
            List.of(new SimpleGrantedAuthority("USER"))
        );
    }

    @Test
    void getTranslations_WhenRealmAndLangExist_ReturnsTranslations() {
        // Given - save existing translation
        repository.save(testEntity);

        // When
        Map<String, String> result = translationService.getTranslations("default", "en");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsAllEntriesOf(testTranslations);
    }

    @Test
    void getTranslations_WhenRealmAndLangNotExist_ReturnsEmptyMap() {
        // When
        Map<String, String> result = translationService.getTranslations("nonexistent", "nonexistent");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void getAvailableLanguagesByRealm_WhenRealmExists_ReturnsLanguages() {
        // Given - save multiple translations for same realm
        TranslationEntity entity1 = new TranslationEntity();
        entity1.setRealm("default");
        entity1.setLang("en");
        entity1.setData(Map.of("hello", "Hello"));
        entity1.setUpdatedAt(Instant.now());
        
        TranslationEntity entity2 = new TranslationEntity();
        entity2.setRealm("default");
        entity2.setLang("fr");
        entity2.setData(Map.of("hello", "Bonjour"));
        entity2.setUpdatedAt(Instant.now());
        
        repository.save(entity1);
        repository.save(entity2);

        // When
        List<String> result = translationService.getAvailableLanguagesByRealm("default");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsExactlyInAnyOrder("en", "fr");
    }

    @Test
    void listRealmsWithLanguages_WhenRealmsExist_ReturnsPagedResponse() {
        // Given - save multiple translations
        repository.save(testEntity);
        
        TranslationEntity entity2 = new TranslationEntity();
        entity2.setRealm("admin");
        entity2.setLang("en");
        entity2.setData(Map.of("admin.title", "Admin Panel"));
        entity2.setUpdatedAt(Instant.now());
        entity2.setUpdatedBy(UUID.randomUUID());
        repository.save(entity2);

        // When
        RealmsPagedResponse result = translationService.listRealmsWithLanguages(
            Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void updateTranslations_WhenNewRealmAndLang_CreatesNewTranslation() {
        // Given
        Map<String, String> newTranslations = Map.of(
            "button.save", "Save",
            "button.cancel", "Cancel"
        );

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getUserPrincipal).thenReturn(testUser);

            // When
            SuccessfulResponse response = translationService.updateTranslations("new-realm", "fr", newTranslations);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getResult()).isTrue();

            // Verify entity was saved to database
            Optional<TranslationEntity> saved = repository.findByRealmAndLang("new-realm", "fr");
            assertThat(saved).isPresent();
            assertThat(saved.get().getData()).containsAllEntriesOf(newTranslations);
            assertThat(saved.get().getUpdatedBy()).isEqualTo(testUser.getUserId());
        }
    }

    @Test
    void updateTranslations_WhenExistingRealmAndLang_UpdatesTranslation() {
        // Given - save existing translation
        repository.save(testEntity);
        
        Map<String, String> updatedTranslations = Map.of(
            "hello", "Hola",
            "goodbye", "Adiós"
        );

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getUserPrincipal).thenReturn(testUser);

            // When
            SuccessfulResponse response = translationService.updateTranslations("default", "en", updatedTranslations);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getResult()).isTrue();

            // Verify database was updated
            Optional<TranslationEntity> updated = repository.findByRealmAndLang("default", "en");
            assertThat(updated).isPresent();
            assertThat(updated.get().getData()).containsAllEntriesOf(updatedTranslations);
            assertThat(updated.get().getUpdatedBy()).isEqualTo(testUser.getUserId());
        }
    }

    @Test
    void getTranslationAdminView_WhenRealmAndLangExist_ReturnsAdminView() {
        // Given - save existing translation
        repository.save(testEntity);

        // When
        Optional<TranslationAdminView> result = translationService.getTranslationAdminView("default", "en");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTranslations()).containsAllEntriesOf(testTranslations);
        assertThat(result.get().getUpdatedBy()).isEqualTo(testEntity.getUpdatedBy());
    }

    @Test
    void getTranslationAdminView_WhenRealmAndLangNotExist_ReturnsEmpty() {
        // When
        Optional<TranslationAdminView> result = translationService.getTranslationAdminView("nonexistent", "nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void deleteTranslation_WhenRealmAndLangExist_DeletesTranslation() {
        // Given - save existing translation
        repository.save(testEntity);

        // When
        SuccessfulResponse response = translationService.deleteTranslation("default", "en");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isTrue();

        // Verify entity was deleted from database
        Optional<TranslationEntity> deleted = repository.findByRealmAndLang("default", "en");
        assertThat(deleted).isEmpty();
    }

    @Test
    void deleteTranslation_WhenRealmAndLangNotExist_ReturnsSuccess() {
        // When
        SuccessfulResponse response = translationService.deleteTranslation("nonexistent", "nonexistent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isTrue();
    }
}
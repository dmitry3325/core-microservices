package com.corems.translationms.app.repository;

import com.corems.common.utils.db.repo.SearchableRepository;
import com.corems.translationms.app.entity.Translation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface TranslationRepository extends SearchableRepository<Translation, Long> {

    Optional<Translation> findByRealmAndLang(String realm, String lang);

    @Override
    default List<String> getSearchFields() {
        return List.of("realm", "lang");
    }

    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("realm", "lang", "updatedBy");
    }

    @Override
    default List<String> getAllowedSortFields() {
        return List.of("updatedAt", "realm", "lang");
    }

    @Override
    default Map<String, String> getFieldAliases() {
        return Map.of();
    }
}

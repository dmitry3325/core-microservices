package com.corems.translationms.app.repository;

import com.corems.common.utils.db.repo.SearchableRepository;
import com.corems.translationms.app.entity.TranslationEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationRepository extends SearchableRepository<TranslationEntity, Long> {

    Optional<TranslationEntity> findByRealmAndLang(String realm, String lang);

    @Query("select distinct t.lang from TranslationEntity t where t.realm = :realm")
    List<String> findDistinctLanguagesByRealm(@Param("realm") String realm);

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

}

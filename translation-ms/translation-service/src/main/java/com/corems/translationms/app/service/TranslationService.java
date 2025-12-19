package com.corems.translationms.app.service;

import com.corems.common.security.SecurityUtils;
import com.corems.translationms.api.model.RealmsPagedResponse;
import com.corems.translationms.api.model.SuccessfulResponse;
import com.corems.translationms.api.model.TranslationAdminView;
import com.corems.translationms.app.entity.TranslationEntity;
import com.corems.translationms.app.repository.TranslationRepository;
import com.corems.translationms.api.model.LanguageInfo;
import com.corems.translationms.api.model.RealmLanguages;
import com.corems.common.utils.db.utils.QueryParams;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final TranslationRepository repository;

    @Transactional(readOnly = true)
    public Map<String, String> getTranslations(String realm, String lang) {
        return repository.findByRealmAndLang(realm, lang)
                .map(TranslationEntity::getData)
                .orElseGet(Collections::emptyMap);
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableLanguagesByRealm(String realm) {
        return repository.findDistinctLanguagesByRealm(realm).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public RealmsPagedResponse listRealmsWithLanguages(Optional<Integer> page,
                                                       Optional<Integer> pageSize,
                                                       Optional<String> search,
                                                       Optional<String> sort) {
        if (sort.isEmpty()) sort = Optional.of("realm:asc");
        QueryParams params = new QueryParams(page, pageSize, search, sort, Optional.empty());
        Page<TranslationEntity> result = repository.findAllByQueryParams(params);

        List<RealmLanguages> realms =  result.stream()
                .collect(Collectors.groupingBy(TranslationEntity::getRealm))
                .entrySet().stream()
                .map(e -> {
                    RealmLanguages rl = new RealmLanguages(e.getKey(), null);
                    List<LanguageInfo> langs = e.getValue().stream()
                            .map(t -> new LanguageInfo(t.getLang(),
                                    OffsetDateTime.ofInstant(t.getUpdatedAt(), ZoneOffset.UTC),
                                    t.getUpdatedBy()))
                            .toList();
                    rl.setLanguages(langs);
                    return rl;
                })
                .toList();
        
        RealmsPagedResponse resp = new RealmsPagedResponse(result.getNumber() + 1, result.getSize());
        resp.setItems(realms);
        resp.setTotalPages(result.getTotalPages());
        resp.setTotalElements(result.getTotalElements());
        return resp;
    }

    @Transactional
    public SuccessfulResponse updateTranslations(String realm, String lang, Map<String, String> translations) {
        Optional<TranslationEntity> existing = repository.findByRealmAndLang(realm, lang);
        TranslationEntity t = existing.orElseGet(() -> {
            TranslationEntity n = new TranslationEntity();
            n.setRealm(realm);
            n.setLang(lang);
            return n;
        });

        t.setData(translations);
        t.setUpdatedAt(Instant.now());
        t.setUpdatedBy(SecurityUtils.getUserPrincipal().getUserId());

        repository.save(t);

        return new SuccessfulResponse().result(true);
    }

    @Transactional(readOnly = true)
    public Optional<TranslationAdminView> getTranslationAdminView(String realm, String lang) {
        return repository.findByRealmAndLang(realm, lang)
                .map(t -> new TranslationAdminView(
                        t.getData(),
                        OffsetDateTime.ofInstant(t.getUpdatedAt(), ZoneOffset.UTC),
                        t.getUpdatedBy()));
    }

    @Transactional
    public SuccessfulResponse deleteTranslation(String realm, String lang) {
        Optional<TranslationEntity> existing = repository.findByRealmAndLang(realm, lang);
        existing.ifPresent(repository::delete);
        return new SuccessfulResponse().result(true);
    }
}

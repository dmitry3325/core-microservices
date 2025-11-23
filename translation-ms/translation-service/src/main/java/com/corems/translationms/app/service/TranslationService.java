package com.corems.translationms.app.service;

import com.corems.common.security.SecurityUtils;
import com.corems.common.security.UserPrincipal;
import com.corems.translationms.api.model.RealmsPagedResponse;
import com.corems.translationms.api.model.TranslationAdminView;
import com.corems.translationms.app.entity.Translation;
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
                .map(Translation::getData)
                .orElseGet(Collections::emptyMap);
    }

    @Transactional(readOnly = true)
    public RealmsPagedResponse listRealmsWithLanguages(Optional<Integer> page,
                                                       Optional<Integer> pageSize,
                                                       Optional<String> search,
                                                       Optional<String> sort) {
        if (sort.isEmpty()) sort = Optional.of("realm:asc");
        QueryParams params = new QueryParams(page, pageSize, search, sort, Optional.empty());
        Page<Translation> result = repository.findAllByQueryParams(params);

        List<RealmLanguages> realms =  result.stream()
                .collect(Collectors.groupingBy(Translation::getRealm))
                .entrySet().stream()
                .map(e -> {
                    RealmLanguages rl = new RealmLanguages(e.getKey(), null);
                    List<LanguageInfo> langs = e.getValue().stream()
                            .map(t -> new LanguageInfo(t.getLang(),
                                    OffsetDateTime.ofInstant(t.getUpdatedAt(), ZoneOffset.UTC),
                                    t.getUpdatedBy()))
                            .collect(Collectors.toList());
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
    public void updateTranslations(String realm, String lang, Map<String, String> translations) {
        Optional<Translation> existing = repository.findByRealmAndLang(realm, lang);
        Translation t = existing.orElseGet(() -> {
            Translation n = new Translation();
            n.setRealm(realm);
            n.setLang(lang);
            return n;
        });

        t.setData(translations);
        t.setUpdatedAt(Instant.now());
        t.setUpdatedBy(SecurityUtils.getUserPrincipal().getUserId());

        repository.save(t);
    }

    @Transactional(readOnly = true)
    public Optional<TranslationAdminView> getTranslationAdminView(String realm, String lang) {
        return repository.findByRealmAndLang(realm, lang)
                .map(t -> new TranslationAdminView(
                        t.getData(),
                        OffsetDateTime.ofInstant(t.getUpdatedAt(), ZoneOffset.UTC),
                        t.getUpdatedBy()));
    }
}

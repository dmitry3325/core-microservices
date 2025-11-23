package com.corems.translationms.app.controller;

import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.RequireRoles;
import com.corems.translationms.api.TranslationAdminApi;
import com.corems.translationms.api.model.RealmsPagedResponse;
import com.corems.translationms.api.model.TranslationAdminView;
import com.corems.translationms.api.model.TranslationUpdateRequest;
import com.corems.translationms.app.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TranslationAdminController implements TranslationAdminApi {

    private final TranslationService service;

    @Override
    @RequireRoles(CoreMsRoles.TRANSLATION_MS_ADMIN)
    public ResponseEntity<TranslationAdminView> getTranslationAdminByRealmAndLang(String realm, String lang) {
        var optView = service.getTranslationAdminView(realm, lang);
        return ResponseEntity.of(optView);
    }

    @Override
    @RequireRoles(CoreMsRoles.TRANSLATION_MS_ADMIN)
    public ResponseEntity<RealmsPagedResponse> realmsList(
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> pageSize,
            @RequestParam Optional<String> sort,
            @RequestParam Optional<String> search) {
        var resp = service.listRealmsWithLanguages(page, pageSize, search, sort);
        return ResponseEntity.ok(resp);
    }

    @Override
    @RequireRoles(CoreMsRoles.TRANSLATION_MS_ADMIN)
    public ResponseEntity<Void> updateTranslationAdminByRealmAndLang(String realm, String lang, TranslationUpdateRequest translationUpdateRequest) {
        Map<String, String> translations = translationUpdateRequest.getTranslations();
        service.updateTranslations(realm, lang, translations);
        return ResponseEntity.noContent().build();
    }
}

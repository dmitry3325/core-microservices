package com.corems.translationms.app.controller;

import com.corems.common.security.CoreMsRoles;
import com.corems.common.security.RequireRoles;
import com.corems.translationms.api.TranslationAdminApi;
import com.corems.translationms.api.model.RealmsPagedResponse;
import com.corems.translationms.api.model.SuccessfulResponse;
import com.corems.translationms.api.model.TranslationAdminView;
import com.corems.translationms.api.model.TranslationUpdateRequest;
import com.corems.translationms.app.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequireRoles(CoreMsRoles.TRANSLATION_MS_ADMIN)
public class TranslationAdminController implements TranslationAdminApi {

    private final TranslationService service;

    @Override
    public ResponseEntity<TranslationAdminView> getTranslationAdminByRealmAndLang(String realm, String lang) {
        return ResponseEntity.of(service.getTranslationAdminView(realm, lang));
    }

    @Override
    public ResponseEntity<RealmsPagedResponse> realmsList(
            Optional<Integer> page,
            Optional<Integer> pageSize,
            Optional<String> sort,
            Optional<String> search) {
        return ResponseEntity.ok(service.listRealmsWithLanguages(page, pageSize, search, sort));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> updateTranslationAdminByRealmAndLang(String realm,
                                                                                   String lang,
                                                                                   TranslationUpdateRequest translationUpdateRequest) {
        return ResponseEntity.ok(service.updateTranslations(realm, lang, translationUpdateRequest.getTranslations()));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> deleteTranslationAdminByRealmAndLang(String realm, String lang) {
        return ResponseEntity.ok(service.deleteTranslation(realm, lang));
    }
}

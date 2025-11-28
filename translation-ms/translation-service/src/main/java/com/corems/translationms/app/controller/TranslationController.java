package com.corems.translationms.app.controller;

import com.corems.translationms.api.TranslationApi;
import com.corems.translationms.app.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TranslationController implements TranslationApi {

    private final TranslationService service;

    @Override
    public ResponseEntity<Map<String, String>> getTranslationByRealmAndLang(String realm, String lang) {
        return ResponseEntity.ok(service.getTranslations(realm, lang));
    }

    @Override
    public ResponseEntity<List<String>> getAvailableLanguagesByRealm(String realm) {
        return ResponseEntity.ok(service.getAvailableLanguagesByRealm(realm));
    }
}

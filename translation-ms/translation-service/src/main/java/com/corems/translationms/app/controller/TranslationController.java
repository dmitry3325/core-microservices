package com.corems.translationms.app.controller;

import com.corems.translationms.api.TranslationApi;
import com.corems.translationms.app.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TranslationController implements TranslationApi {

    private final TranslationService service;

    @Override
    public ResponseEntity<Map<String, String>> getTranslationByRealmAndLang(String realm, String lang) {
        Map<String, String> translations = service.getTranslations(realm, lang);
        return ResponseEntity.ok(translations);
    }
}


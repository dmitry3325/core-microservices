package com.corems.translationms.client;

import com.corems.translationms.ApiClient;
import com.corems.translationms.client.TranslationAdminApi;
import com.corems.translationms.client.TranslationApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
public class TranslationMsClientConfig {

    @Value("${translation.ms.base-url:http://localhost:3003}")
    private String translationMsBaseUrl;

    @Bean(name = "translationMsWebClient")
    @ConditionalOnMissingBean(name = "translationMsWebClient")
    public WebClient translationMsWebClient(WebClient.Builder inboundWebClientBuilder) {
        return inboundWebClientBuilder
                .baseUrl(translationMsBaseUrl)
                .build();
    }

    @Bean(name = "translationMsApiClient")
    @ConditionalOnMissingBean(name = "translationMsApiClient")
    public ApiClient translationMsApiClient(@Qualifier("translationMsWebClient") WebClient translationMsWebClient) {
        ApiClient apiClient = new ApiClient(translationMsWebClient);
        apiClient.setBasePath(translationMsBaseUrl);
        return apiClient;
    }

    @Bean
    @ConditionalOnMissingBean(TranslationApi.class)
    public TranslationApi translationApi(ApiClient translationMsApiClient) {
        return new TranslationApi(translationMsApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(TranslationAdminApi.class)
    public TranslationAdminApi translationAdminApi(ApiClient translationMsApiClient) {
        return new TranslationAdminApi(translationMsApiClient);
    }

    // Add beans for generated API interfaces as needed, e.g.:
    // @Bean
    // @ConditionalOnMissingBean(TranslationsApi.class)
    // public TranslationsApi translationsApi(ApiClient translationMsApiClient) {
    //     return new TranslationsApi(translationMsApiClient);
    // }
}

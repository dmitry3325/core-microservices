package com.corems.documentms.client;

import com.corems.documentms.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class DocumentMsClientConfig {

    @Value("${documentms.base-url:http://localhost:3002}")
    private String documentMsBaseUrl;

    @Bean(name = "documentRestClient")
    @ConditionalOnMissingBean(name = "documentRestClient")
    public RestClient documentRestClient(RestClient.Builder inboundRestClientBuilder) {
        return inboundRestClientBuilder
                .baseUrl(documentMsBaseUrl)
                .build();
    }

    @Bean(name = "documentApiClient")
    @ConditionalOnMissingBean(name = "documentApiClient")
    public ApiClient documentApiClient(RestClient documentRestClient) {
        ApiClient apiClient = new ApiClient(documentRestClient);
        apiClient.setBasePath(documentMsBaseUrl);
        return apiClient;
    }

    @Bean
    @ConditionalOnMissingBean(DocumentApi.class)
    public DocumentApi documentApi(ApiClient documentApiClient) {
        return new DocumentApi(documentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean(DocumentsListApi.class)
    public DocumentsListApi documentsListApi(ApiClient documentApiClient) {
        return new DocumentsListApi(documentApiClient);
    }
}


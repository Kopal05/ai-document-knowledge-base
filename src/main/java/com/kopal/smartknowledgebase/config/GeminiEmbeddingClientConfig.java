package com.kopal.smartknowledgebase.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Provides the RestClient bean used to call Gemini's embedding endpoint.
 *
 * Same separation-of-concerns reasoning as EmbeddingClientConfig: this
 * class owns "how the HTTP client is configured" (base URL + the
 * x-goog-api-key header Gemini requires on every request), so
 * GeminiEmbeddingProviderClient only has to know "how to call this one
 * specific endpoint" — identical division of responsibility, just with
 * one extra default header Ollama's local setup never needed.
 */
@Configuration
public class GeminiEmbeddingClientConfig {

    @Bean
    public RestClient geminiEmbeddingRestClient(
            @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${gemini.api.key}") String apiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }
}
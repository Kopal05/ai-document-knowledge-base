package com.kopal.smartknowledgebase.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Provides the RestClient bean used to call the embedding provider.
 *
 * WHY THIS FILE EXISTS HERE:
 * This is the config package's first real file — it was left empty on
 * purpose back in Phase 1, reserved for exactly this kind of thing:
 * defining beans that don't belong to any single business class.
 *
 * WHY A SEPARATE @Bean METHOD INSTEAD OF BUILDING RestClient DIRECTLY
 * INSIDE OllamaEmbeddingProviderClient's CONSTRUCTOR:
 * Building it here and injecting it means OllamaEmbeddingProviderClient
 * receives an already-configured RestClient rather than constructing one
 * itself — that keeps "how the HTTP client is configured" (base URL)
 * separate from "how we use it to call Ollama's specific endpoint"
 * (OllamaEmbeddingProviderClient's job). It also means a test could
 * inject a completely different RestClient (e.g. pointed at a mock
 * server) without changing OllamaEmbeddingProviderClient's code at all.
 */
@Configuration
public class EmbeddingClientConfig {

    @Bean
    public RestClient embeddingRestClient(@Value("${embedding.provider.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
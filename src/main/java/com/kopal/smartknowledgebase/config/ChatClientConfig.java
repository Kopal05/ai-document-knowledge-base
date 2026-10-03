package com.kopal.smartknowledgebase.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Provides the RestClient bean used to call the chat/answer-generation
 * provider — a SEPARATE bean from EmbeddingClientConfig's
 * embeddingRestClient, deliberately.
 *
 * WHY A SEPARATE RestClient, NOT REUSE embeddingRestClient:
 * The task requires the chat model and base URL to be independently
 * configurable from the embedding provider's. Today both default to the
 * same local Ollama instance (http://localhost:11434), but they're two
 * distinct configuration values (chat.provider.base-url vs
 * embedding.provider.base-url) — if someone points chat at a different
 * host/port later (a bigger model on a separate machine, a different
 * provider entirely), a shared RestClient with one hardcoded base URL
 * couldn't support that at all.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public RestClient chatRestClient(@Value("${chat.provider.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
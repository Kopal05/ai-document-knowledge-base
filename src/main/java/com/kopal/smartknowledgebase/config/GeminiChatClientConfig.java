package com.kopal.smartknowledgebase.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Provides the RestClient bean used to call Gemini's chat/generation
 * endpoint — a separate bean from GeminiEmbeddingClientConfig's
 * geminiEmbeddingRestClient purely to mirror the existing
 * ChatClientConfig/EmbeddingClientConfig split (two distinct injectable
 * bean names, matched by parameter name in each provider client's
 * constructor). Unlike Ollama's two genuinely-different base URLs,
 * Gemini's embedding and chat endpoints share one host — both beans
 * read the same gemini.api.base-url and gemini.api.key, so there's no
 * duplicated configuration value, just two distinctly-named beans.
 */
@Configuration
public class GeminiChatClientConfig {

    @Bean
    public RestClient geminiChatRestClient(
            @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${gemini.api.key}") String apiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }
}
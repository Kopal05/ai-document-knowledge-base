package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.exception.EmbeddingGenerationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Talks to a locally-running Ollama server to generate embeddings using
 * the nomic-embed-text model.
 *
 * WHY @Component INSTEAD OF @Service:
 * Both annotations register a Spring bean identically — there's no
 * functional difference. @Service is used elsewhere in this project
 * (DocumentService, TextChunkingService, etc.) to mark core business
 * logic. This class isn't business logic — it's an adapter to a specific
 * external system, so @Component is the more accurate label for what it
 * actually is: infrastructure, not domain behavior.
 *
 * WHY THIS CLASS OWNS THE REQUEST/RESPONSE SHAPE:
 * EmbedRequest and EmbedResponse below are private nested records that
 * exist ONLY to match Ollama's specific JSON wire format
 * (POST /api/embed expects {"model": "...", "input": "..."} and returns
 * {"embeddings": [[...]]} — an array of vectors, since the endpoint
 * supports batching multiple inputs at once; we only ever send one, so
 * we always read index 0). These are deliberately NOT in the shared dto
 * package — dto/ represents OUR API's contract with ITS callers; these
 * records represent Ollama's contract with US, a completely different,
 * provider-specific concern that has no business being visible anywhere
 * else in the codebase.
 */
@Component
public class OllamaEmbeddingProviderClient implements EmbeddingProviderClient {

    private final RestClient embeddingRestClient;
    private final String model;

    // Constructor injection: the RestClient bean comes from
    // EmbeddingClientConfig (already configured with the provider's base
    // URL); the model name is read directly from configuration here
    // since it's specific to how THIS provider is called, not something
    // EmbeddingService needs to know about.
    public OllamaEmbeddingProviderClient(RestClient embeddingRestClient,
                                         @Value("${embedding.provider.model}") String model) {
        this.embeddingRestClient = embeddingRestClient;
        this.model = model;
    }

    @Override
    public List<Double> embed(String text) {
        EmbedRequest requestBody = new EmbedRequest(model, text);

        EmbedResponse response;
        try {
            response = embeddingRestClient.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(EmbedResponse.class);
        } catch (RestClientException e) {
            // Covers connection failures (Ollama not running, wrong URL)
            // and non-2xx HTTP responses. We don't want Spring's
            // RestClientException type — an HTTP-client-library-specific
            // type — leaking out of this class; EmbeddingGenerationException
            // is our own stable, meaningful contract instead.
            throw new EmbeddingGenerationException(
                    "Failed to reach the embedding provider (model: " + model + ")", e);
        }

        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new EmbeddingGenerationException("Embedding provider returned an empty response");
        }

        // /api/embed is batch-capable and always returns a list of
        // vectors, one per input we sent. We only ever send one input,
        // so the vector we want is always at index 0.
        return response.embeddings().get(0);
    }

    private record EmbedRequest(String model, String input) {
    }

    private record EmbedResponse(List<List<Double>> embeddings) {
    }
}
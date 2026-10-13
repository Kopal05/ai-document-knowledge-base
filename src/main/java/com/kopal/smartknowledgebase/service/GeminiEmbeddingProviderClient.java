package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.exception.EmbeddingGenerationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Talks to Google's Gemini API to generate embeddings, selected only
 * when the "prod" profile is active (see application.properties and
 * GeminiEmbeddingClientConfig).
 *
 * WIRE FORMAT — verified directly against Google's current REST API
 * docs (ai.google.dev/api/embeddings), not assumed:
 * POST /v1beta/models/{model}:embedContent
 * Request:  {"model": "models/...", "content": {"parts": [{"text": "..."}]},
 *            "embedContentConfig": {"outputDimensionality": 768}}
 * Response: {"embedding": {"values": [...], "shape": [768]}}
 *
 * WHY outputDimensionality IS HARDCODED TO DocumentChunk.EMBEDDING_DIMENSIONS,
 * NOT A SEPARATE CONFIG PROPERTY:
 * This number is NOT a tunable setting — it's a direct consequence of
 * the already-deployed pgvector column being vector(768). Making it a
 * separate property would let someone change it without touching the
 * schema, producing a silent dimension mismatch at the worst possible
 * time (a failed or corrupted vector write). Importing the constant
 * from DocumentChunk keeps the two permanently in lock-step, the same
 * way the comment in DocumentChunk itself already warns they must be.
 */
@Component
@Profile("prod")
public class GeminiEmbeddingProviderClient implements EmbeddingProviderClient {

    private final RestClient geminiEmbeddingRestClient;
    private final String model;

    public GeminiEmbeddingProviderClient(RestClient geminiEmbeddingRestClient,
                                         @Value("${gemini.embedding.model:gemini-embedding-001}") String model) {
        this.geminiEmbeddingRestClient = geminiEmbeddingRestClient;
        this.model = model;
    }

    @Override
    public List<Double> embed(String text) {
        EmbedContentRequest requestBody = new EmbedContentRequest(
                "models/" + model,
                new Content(List.of(new Part(text))),
                new EmbedContentConfig(com.kopal.smartknowledgebase.entity.DocumentChunk.EMBEDDING_DIMENSIONS));

        EmbedContentResponse response;
        try {
            response = geminiEmbeddingRestClient.post()
                    .uri("/v1beta/models/{model}:embedContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(EmbedContentResponse.class);
        } catch (RestClientException e) {
            throw new EmbeddingGenerationException(
                    "Failed to reach the Gemini embedding provider (model: " + model + ")", e);
        }

        if (response == null || response.embedding() == null || response.embedding().values() == null
                || response.embedding().values().isEmpty()) {
            throw new EmbeddingGenerationException("Gemini embedding provider returned an empty response");
        }

        List<Double> rawValues = response.embedding().values();
        int targetDimensions = com.kopal.smartknowledgebase.entity.DocumentChunk.EMBEDDING_DIMENSIONS;

        // Gemini's "outputDimensionality" request parameter is not
        // reliably honored by the API (confirmed by actual testing: it
        // returned the full 3072-dim vector despite the request asking
        // for 768). gemini-embedding-001 uses Matryoshka Representation
        // Learning, which makes this safe to handle ourselves: the first
        // N values of the full embedding ARE a valid N-dimensional
        // embedding by construction. We truncate client-side instead of
        // depending on a server-side parameter that doesn't behave as
        // documented.
        if (rawValues.size() < targetDimensions) {
            throw new EmbeddingGenerationException(
                    "Gemini embedding provider returned " + rawValues.size()
                            + " dimensions, which is fewer than the required " + targetDimensions);
        }
        List<Double> truncated = rawValues.subList(0, targetDimensions);

        // Only the full 3072-dim output is pre-normalized by Gemini's
        // API; a truncated vector must be re-normalized (L2 norm) so
        // pgvector's similarity search compares embeddings on equal
        // footing, same as it already does for Ollama's embeddings.
        double sumOfSquares = 0.0;
        for (Double value : truncated) {
            if (value == null || value.isNaN() || value.isInfinite()) {
                throw new EmbeddingGenerationException("Gemini embedding provider returned a non-finite value");
            }
            sumOfSquares += value * value;
        }
        double norm = Math.sqrt(sumOfSquares);
        if (norm == 0.0) {
            throw new EmbeddingGenerationException("Gemini embedding provider returned a zero vector");
        }

        List<Double> normalized = new java.util.ArrayList<>(targetDimensions);
        for (Double value : truncated) {
            normalized.add(value / norm);
        }

        return normalized;
    }

    // Wire-format records specific to Gemini's embedContent shape —
    // deliberately not in the shared dto package, same reasoning as
    // OllamaEmbeddingProviderClient's EmbedRequest/EmbedResponse.
    private record EmbedContentRequest(String model, Content content, EmbedContentConfig embedContentConfig) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record EmbedContentConfig(int outputDimensionality) {
    }

    private record EmbedContentResponse(Embedding embedding) {
    }

    private record Embedding(List<Double> values) {
    }
}
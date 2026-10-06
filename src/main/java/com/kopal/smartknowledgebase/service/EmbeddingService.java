package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.exception.EmbeddingGenerationException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Turns a piece of text into an embedding vector.
 *
 * WHY THIS IS A SEPARATE SERVICE:
 * Same single-responsibility reasoning as PdfTextExtractionService and
 * TextChunkingService: "how do we get a semantic vector for this text"
 * is one clear, independent concern. This class knows NOTHING about
 * Document, DocumentChunk, repositories, or PostgreSQL — it isn't wired
 * into DocumentService yet, deliberately, so it can be built and tested
 * entirely on its own first, exactly like the other two services were.
 *
 * WHY THIS DOESN'T TALK TO RestClient/OLLAMA DIRECTLY:
 * It depends on EmbeddingProviderClient (an interface), not on
 * OllamaEmbeddingProviderClient or RestClient. This class has no idea
 * which provider is actually being used underneath — that's the whole
 * point of the abstraction (see EmbeddingProviderClient's javadoc).
 */
@Service
public class EmbeddingService {

    private final EmbeddingProviderClient embeddingProviderClient;

    public EmbeddingService(EmbeddingProviderClient embeddingProviderClient) {
        this.embeddingProviderClient = embeddingProviderClient;
    }

    /**
     * Generates an embedding vector for the given text.
     *
     * @param text the text to embed — must not be null or blank
     * @return the embedding as a float[], one element per dimension
     * @throws IllegalArgumentException if text is null or blank
     * @throws com.kopal.smartknowledgebase.exception.EmbeddingGenerationException
     *         if the provider call fails or returns an unusable response
     */
    public float[] generateEmbedding(String text) {
        // Unlike TextChunkingService, blank text isn't treated as a
        // valid "nothing to do" case here — there's no meaningful
        // "embedding of nothing." A caller should decide NOT to call
        // this method for empty content; if they do anyway, that's a
        // caller mistake, so we fail fast just like we do for null.
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }

        List<Double> rawVector;
        try {
            rawVector = embeddingProviderClient.embed(text);
        } catch (EmbeddingGenerationException e) {
            // Already the right exception type (e.g. the client itself
            // detected a bad response) — don't double-wrap it.
            throw e;
        } catch (Exception e) {
            // Anything else (network failure, timeout, etc.) is an
            // infrastructure problem the caller shouldn't have to know
            // the shape of — wrap it in our own exception type.
            throw new EmbeddingGenerationException("Unexpected error generating embedding", e);
        }

        if (rawVector == null || rawVector.isEmpty()) {
            throw new EmbeddingGenerationException("Embedding provider returned no data");
        }

        // This is the one place the raw provider response (List<Double>)
        // becomes our chosen internal representation (float[]) — see the
        // "vector representation" design discussion for why float[].
        float[] embedding = new float[rawVector.size()];
        for (int i = 0; i < rawVector.size(); i++) {
            embedding[i] = rawVector.get(i).floatValue();
        }
        return embedding;
    }
}
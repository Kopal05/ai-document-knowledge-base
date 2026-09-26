package com.kopal.smartknowledgebase.exception;

/**
 * Thrown when generating an embedding fails — the provider couldn't be
 * reached, returned an error, or returned something we didn't expect.
 *
 * WHY THIS FILE EXISTS:
 * Same pattern as PdfTextExtractionException: OllamaEmbeddingProviderClient
 * catches the low-level failure (a network error, an HTTP error status,
 * an unparseable response) and rethrows it as this one, unchecked,
 * domain-specific exception. Callers of EmbeddingService never need to
 * know or handle Spring's RestClientException type directly — they only
 * need to know "embedding generation can fail," which is a much more
 * stable contract than "whatever exception type this particular HTTP
 * client library happens to throw."
 */
public class EmbeddingGenerationException extends RuntimeException {

    public EmbeddingGenerationException(String message) {
        super(message);
    }

    public EmbeddingGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
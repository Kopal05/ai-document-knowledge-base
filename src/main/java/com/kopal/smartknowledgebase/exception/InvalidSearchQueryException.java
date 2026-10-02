package com.kopal.smartknowledgebase.exception;

/**
 * Thrown when a semantic search request's query text is missing or blank.
 *
 * WHY THIS EXISTS INSTEAD OF JUST RELYING ON EmbeddingService's OWN CHECK:
 * EmbeddingService.generateEmbedding(...) already throws
 * IllegalArgumentException for null/blank text — that guard still exists
 * and still matters as a defensive backstop. But SemanticSearchService
 * validates the query FIRST, before ever calling EmbeddingService, for
 * the same reason DocumentService.validatePdfFile() validates an upload
 * before calling PdfTextExtractionService: validation that's specific to
 * THIS caller's context (a search query, with a message that makes sense
 * to someone calling the search API) belongs at the layer that knows
 * it's a search request — not borrowed from a lower-level service whose
 * error message ("text must not be blank") doesn't mention search at
 * all. This mirrors InvalidFileException's role for the upload flow.
 */
public class InvalidSearchQueryException extends RuntimeException {

    public InvalidSearchQueryException(String message) {
        super(message);
    }
}
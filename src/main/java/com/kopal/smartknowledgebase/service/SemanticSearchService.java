package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.SearchResponse;
import com.kopal.smartknowledgebase.dto.SearchResult;
import com.kopal.smartknowledgebase.exception.DocumentNotFoundException;
import com.kopal.smartknowledgebase.exception.InvalidSearchQueryException;
import com.kopal.smartknowledgebase.repository.DocumentChunkRepository;
import com.kopal.smartknowledgebase.repository.DocumentChunkSearchProjection;
import com.kopal.smartknowledgebase.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates semantic search: turn a natural-language query into an
 * embedding, then find the most similar DocumentChunks via pgvector.
 *
 * WHY THIS IS ITS OWN SERVICE, NOT A METHOD ON DocumentService:
 * DocumentService already orchestrates document-time ingestion (upload,
 * extract, chunk, embed, persist). Semantic search is a different
 * concern — query-time retrieval, with a different shape of work
 * entirely — reusing EmbeddingService but for a completely different
 * purpose than DocumentService uses it for. Keeping this separate avoids
 * DocumentService growing into a do-everything class, and matches the
 * one-class-one-job pattern every other service in this project already
 * follows (PdfTextExtractionService, TextChunkingService, EmbeddingService).
 */
@Service
public class SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final int defaultTopK;

    public SemanticSearchService(EmbeddingService embeddingService,
                                 DocumentChunkRepository documentChunkRepository,
                                 DocumentRepository documentRepository,
                                 @Value("${search.default-top-k:5}") int defaultTopK) {
        this.embeddingService = embeddingService;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
        this.defaultTopK = defaultTopK;
    }

    /**
     * @param documentId the document to search within
     * @param query      the user's natural-language search text
     * @param topK       how many results to return; null or <= 0 falls
     *                   back to the configured default (search.default-top-k)
     */
    public SearchResponse search(Long documentId, String query, Integer topK) {
        if (query == null || query.isBlank()) {
            throw new InvalidSearchQueryException("Search query must not be blank");
        }

        // Reuses the exact same existence check DocumentService.deleteDocument
        // already relies on — searching against a document that was never
        // created is a 404, not a confusing "zero results" response.
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }

        int effectiveTopK = (topK != null && topK > 0) ? topK : defaultTopK;

        // REUSE the existing EmbeddingService exactly as-is — same
        // service DocumentService calls for document chunks, now called
        // for the query text instead. No new embedding client, no
        // duplicated Ollama logic.
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        String queryVectorLiteral = toPgVectorLiteral(queryEmbedding);

        List<DocumentChunkSearchProjection> rows =
                documentChunkRepository.searchSimilarChunks(documentId, queryVectorLiteral, effectiveTopK);

        List<SearchResult> results = rows.stream()
                .map(row -> new SearchResult(
                        row.getId(),
                        row.getDocumentId(),
                        row.getChunkIndex(),
                        row.getChunkText(),
                        row.getSimilarity()))
                .toList();

        return new SearchResponse(query, results);
    }

    /**
     * Converts a Java float[] into pgvector's plain-text literal format,
     * e.g. [0.123,-0.421,0.087]. This exact format is what lets the
     * native query's CAST(:queryVector AS vector) work — see
     * DocumentChunkRepository.searchSimilarChunks for why a String is
     * used here instead of binding the float[] directly.
     */
    private String toPgVectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder(vector.length * 8);
        builder.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        builder.append(']');
        return builder.toString();
    }
}
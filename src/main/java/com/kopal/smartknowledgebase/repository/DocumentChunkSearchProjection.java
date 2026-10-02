package com.kopal.smartknowledgebase.repository;

/**
 * Shape of one row returned by DocumentChunkRepository.searchSimilarChunks.
 *
 * WHY THIS INTERFACE EXISTS:
 * The native similarity-search query doesn't just read existing
 * DocumentChunk columns — it also computes a new value, `similarity`,
 * that has no corresponding entity field. Spring Data JPA can't map a
 * result like that onto the DocumentChunk entity (entities only map to
 * real columns), so this is an "interface-based projection" instead: a
 * plain interface whose getter methods Spring Data matches, by name, to
 * the native query's result-set column aliases. Each getter below
 * corresponds EXACTLY to an `AS xxx` alias in the native query in
 * DocumentChunkRepository — that naming match is what makes this work.
 *
 * This stays in the repository package (not dto/) because it's purely a
 * JDBC-result-shape detail of ONE query, not part of this API's public
 * contract — SemanticSearchService immediately converts rows of this
 * into SearchResult (the real DTO) before anything leaves the service
 * layer.
 */
public interface DocumentChunkSearchProjection {

    Long getId();

    Long getDocumentId();

    Integer getChunkIndex();

    String getChunkText();

    Double getSimilarity();
}
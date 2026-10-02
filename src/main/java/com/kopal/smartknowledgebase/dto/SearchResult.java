package com.kopal.smartknowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One matched chunk from a semantic search, as returned to the client.
 *
 * WHY THESE FIELDS, AND NOTHING ELSE:
 * chunkId/documentId/chunkIndex/chunkText give the caller enough to
 * identify and display the source of a match (which document, which
 * part of it, and the actual text). similarity is the ranking signal.
 *
 * WHY THE RAW 768-DIMENSION VECTOR IS NEVER INCLUDED HERE:
 * The embedding is an internal implementation detail of HOW we found
 * this match, not something a caller of this API needs or should see —
 * same reasoning Document/DocumentResponse already follows: never expose
 * internals the client has no use for. (It would also be a meaningless
 * wall of 768 numbers to anyone reading the response.)
 *
 * similarity is a double because pgvector's distance/similarity math
 * naturally produces fractional values; it mirrors the "similarity":
 * 0.91-style value in the example response — see
 * DocumentChunkRepository's query for exactly how it's computed
 * (1 - cosine distance).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private Long chunkId;
    private Long documentId;
    private Integer chunkIndex;
    private String chunkText;
    private Double similarity;
}
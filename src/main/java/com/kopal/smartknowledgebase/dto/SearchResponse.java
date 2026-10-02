package com.kopal.smartknowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response body for GET /api/documents/{documentId}/search.
 *
 * WHY WRAP results IN AN OBJECT INSTEAD OF RETURNING A BARE LIST:
 * Echoing the original query back alongside the results costs nothing
 * and is genuinely useful: a client rendering search results can show
 * "results for: ..." without having to remember what it sent, and if
 * this endpoint ever needs to add fields later (total match count,
 * timing info, etc.) there's already a wrapper object to add them to —
 * a bare JSON array has no such room.
 *
 * results is an empty list (not null, not an error) when no chunks in
 * the document have embeddings yet, or none are similar enough to make
 * the topK cut — "no matches" is a normal, valid search outcome, not a
 * failure. See SemanticSearchService for where that's decided.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private String query;
    private List<SearchResult> results;
}
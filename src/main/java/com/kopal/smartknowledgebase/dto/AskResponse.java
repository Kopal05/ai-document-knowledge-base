package com.kopal.smartknowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response body for POST /api/documents/{documentId}/ask.
 *
 * WHY sources IS List<SearchResult>, NOT A NEW DTO:
 * SearchResult already has exactly the right shape for a source
 * reference — chunkId, documentId, chunkIndex, chunkText, similarity.
 * Introducing a near-duplicate "SourceReference" DTO that differs from
 * SearchResult by nothing meaningful would be the exact kind of
 * duplication this phase was told to avoid. Including chunkText (not
 * just the numeric IDs) is a deliberate choice beyond the bare minimum
 * asked for: it lets a caller show WHICH text the answer is actually
 * grounded in, not just abstract references — useful for a user to
 * verify/trust the answer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AskResponse {

    private String question;
    private String answer;
    private List<SearchResult> sources;
}
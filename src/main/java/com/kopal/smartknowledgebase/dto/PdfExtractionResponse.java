package com.kopal.smartknowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response body for POST /api/documents/upload.
 *
 * WHY THIS FILE EXISTS / WHAT CHANGED THIS PHASE:
 * Previously this only reported {fileName, text} — the upload didn't
 * persist anything, so there was nothing else to report. Now that
 * uploading also creates a Document and its DocumentChunks, the response
 * grows to tell the caller what was actually persisted:
 *   - documentId: so the caller (or a future "get chunks for document X"
 *     endpoint) can refer back to what was just created
 *   - chunkCount: a quick sanity check that chunking behaved as expected,
 *     without the caller having to count entries in `text` themselves
 * fileName and text are unchanged from before.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PdfExtractionResponse {

    private Long documentId;
    private String fileName;
    private String text;
    private int chunkCount;
}
package com.kopal.smartknowledgebase.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits a block of text into smaller, overlapping chunks.
 *
 * WHY THIS IS A SEPARATE SERVICE:
 * PdfTextExtractionService's job ends the moment it hands back a String.
 * "How do we split that String into pieces an LLM/embedding model can
 * work with" is a completely different concern, with its own rules and
 * its own reasons to change independently — so it gets its own class,
 * the same way DocumentService and PdfTextExtractionService are already
 * separated. This service is intentionally NOT wired into DocumentService
 * yet; it's built and tested on its own first.
 */
@Service
public class TextChunkingService {

    // Named constants instead of magic numbers scattered across the
    // codebase. These are the starting values from the task; callers who
    // don't care yet can just call chunkText(text) and get sane defaults.
    public static final int DEFAULT_CHUNK_SIZE = 500;
    public static final int DEFAULT_OVERLAP = 50;

    /**
     * Splits text into chunks using the default chunk size and overlap.
     */
    public List<String> chunkText(String text) {
        return chunkText(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * Splits text into fixed-size, overlapping chunks.
     *
     * @param text      the text to split
     * @param chunkSize the maximum number of characters per chunk (must be > 0)
     * @param overlap   how many trailing characters of one chunk are
     *                  repeated at the start of the next (must be >= 0
     *                  and strictly less than chunkSize)
     * @return the chunks in original order; an empty list if text is
     *         empty or blank
     * @throws IllegalArgumentException if text is null, or chunkSize/overlap
     *                                  are out of range
     */
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        // null is treated as a caller mistake (you should always have a
        // String, even an empty one, before calling this) — fail fast.
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than 0");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("overlap must not be negative");
        }
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap must be smaller than chunkSize");
        }

        // Blank text (empty, or only whitespace — e.g. a scanned/
        // image-only PDF PDFBox couldn't pull any real text from) isn't
        // an error, it's just "nothing to chunk" — so we return an empty
        // list rather than throwing.
        if (text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();

        // step is how far the START of each chunk moves forward. Because
        // we already validated overlap < chunkSize above, step is
        // guaranteed to be > 0 — that guarantee is exactly what stops
        // this loop from ever running forever (see explanation below).
        int step = chunkSize - overlap;
        int textLength = text.length();
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            chunks.add(text.substring(start, end));

            // We just included the last character of the text in this
            // chunk, so there's nothing left to start a new chunk with.
            if (end == textLength) {
                break;
            }

            start += step;
        }

        return chunks;
    }
}
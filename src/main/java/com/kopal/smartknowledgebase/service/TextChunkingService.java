package com.kopal.smartknowledgebase.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TextChunkingService {

    public static final int DEFAULT_CHUNK_SIZE = 500;
    public static final int DEFAULT_OVERLAP = 50;

    public List<String> chunkText(String text) {
        return chunkText(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<String> chunkText(String text, int chunkSize, int overlap) {
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
        if (text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        int textLength = text.length();
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            chunks.add(text.substring(start, end));
            if (end == textLength) {
                break;
            }
            start += step;
        }

        return chunks;
    }
}
package com.kopal.smartknowledgebase.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextChunkingServiceTest {

    private final TextChunkingService service = new TextChunkingService();

    @Test
    void chunksTextWithDefaultSizeAndOverlap() {
        String text = "a".repeat(1200);

        List<String> chunks = service.chunkText(text);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(500);
        assertThat(chunks.get(1)).hasSize(500);
    }

    @Test
    void returnsEmptyListForBlankText() {
        assertThat(service.chunkText("   ")).isEmpty();
    }

    @Test
    void throwsExceptionForNullText() {
        assertThatThrownBy(() -> service.chunkText(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text must not be null");
    }

    @Test
    void throwsExceptionForNonPositiveChunkSize() {
        assertThatThrownBy(() -> service.chunkText("some text", 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize must be greater than 0");
    }

    @Test
    void throwsExceptionForNegativeOverlap() {
        assertThatThrownBy(() -> service.chunkText("some text", 100, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap must not be negative");
    }

    @Test
    void throwsExceptionWhenOverlapGreaterThanOrEqualChunkSize() {
        assertThatThrownBy(() -> service.chunkText("some text", 100, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap must be smaller than chunkSize");
    }

    @Test
    void producesSingleChunkWhenTextShorterThanChunkSize() {
        String text = "short text";

        List<String> chunks = service.chunkText(text, 500, 50);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text);
    }

    @Test
    void lastChunkCapturesRemainderWithoutPaddingBeyondTextLength() {
        String text = "a".repeat(1000);

        List<String> chunks = service.chunkText(text, 500, 50);

        assertThat(chunks).allMatch(c -> c.length() <= 500);
    }
}
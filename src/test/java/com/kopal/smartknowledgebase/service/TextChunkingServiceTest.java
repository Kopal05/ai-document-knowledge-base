package com.kopal.smartknowledgebase.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TextChunkingService.
 *
 * WHY NO @SpringBootTest / NO @ExtendWith(MockitoExtension.class):
 * TextChunkingService has zero dependencies to mock — it's pure logic
 * (String in, List<String> out). So the simplest possible test is just
 * `new TextChunkingService()` and calling methods on it directly, no
 * Spring context and no Mockito needed at all.
 *
 * These tests check exact chunk boundaries and content (not just "the
 * list isn't empty"), because the whole point of this service is
 * getting those boundaries right.
 */
class TextChunkingServiceTest {

    private final TextChunkingService service = new TextChunkingService();

    @Test
    void chunkText_shouldReturnOneChunkWhenTextIsShorterThanChunkSize() {
        List<String> chunks = service.chunkText("short text", 500, 50);

        assertThat(chunks).containsExactly("short text");
    }

    @Test
    void chunkText_shouldReturnOneChunkWhenTextIsExactlyChunkSize() {
        String text = "a".repeat(10);

        List<String> chunks = service.chunkText(text, 10, 3);

        assertThat(chunks).containsExactly(text);
    }

    @Test
    void chunkText_shouldSplitTextLargerThanChunkSizeIntoMultipleChunks() {
        // 20 characters, chunkSize 10, overlap 3 -> step 7
        String text = "ABCDEFGHIJKLMNOPQRST";

        List<String> chunks = service.chunkText(text, 10, 3);

        // Worked out by hand: [0,10), [7,17), [14,20)
        assertThat(chunks).containsExactly(
                "ABCDEFGHIJ",
                "HIJKLMNOPQ",
                "OPQRST"
        );
    }

    @Test
    void chunkText_shouldOverlapAdjacentChunksByTheConfiguredAmount() {
        String text = "ABCDEFGHIJKLMNOPQRST";

        List<String> chunks = service.chunkText(text, 10, 3);

        String endOfFirstChunk = chunks.get(0).substring(chunks.get(0).length() - 3);
        String startOfSecondChunk = chunks.get(1).substring(0, 3);

        assertThat(endOfFirstChunk).isEqualTo(startOfSecondChunk).isEqualTo("HIJ");
    }

    @Test
    void chunkText_shouldReturnEmptyListForEmptyString() {
        assertThat(service.chunkText("", 500, 50)).isEmpty();
    }

    @Test
    void chunkText_shouldReturnEmptyListForBlankString() {
        assertThat(service.chunkText("   \n\t  ", 500, 50)).isEmpty();
    }

    @Test
    void chunkText_shouldThrowForNullInput() {
        assertThatThrownBy(() -> service.chunkText(null, 500, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text");
    }

    @Test
    void chunkText_shouldProduceNonOverlappingBackToBackChunksWhenOverlapIsZero() {
        String text = "0123456789"; // 10 chars

        List<String> chunks = service.chunkText(text, 5, 0);

        assertThat(chunks).containsExactly("01234", "56789");
    }

    @Test
    void chunkText_shouldThrowWhenOverlapIsGreaterThanOrEqualToChunkSize() {
        assertThatThrownBy(() -> service.chunkText("some text", 10, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");

        assertThatThrownBy(() -> service.chunkText("some text", 10, 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void chunkText_shouldThrowWhenChunkSizeIsZeroOrNegative() {
        assertThatThrownBy(() -> service.chunkText("some text", 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize");

        assertThatThrownBy(() -> service.chunkText("some text", -5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize");
    }

    @Test
    void chunkText_shouldThrowWhenOverlapIsNegative() {
        assertThatThrownBy(() -> service.chunkText("some text", 10, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void chunkText_singleArgOverload_shouldUseDefaultChunkSizeAndOverlap() {
        // 550 chars of 'a', with the default 500/50 settings:
        // chunk 1 = [0,500), chunk 2 = [450,550)
        String text = "a".repeat(550);

        List<String> chunks = service.chunkText(text);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).hasSize(500);
        assertThat(chunks.get(1)).hasSize(100);
    }
}
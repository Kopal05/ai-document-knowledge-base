package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.exception.EmbeddingGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmbeddingService.
 *
 * WHY WE MOCK EmbeddingProviderClient (NOT RestClient DIRECTLY):
 * The task requires mocking "the external embedding provider/client" and
 * never making a real network call in a unit test. EmbeddingService
 * depends on the EmbeddingProviderClient INTERFACE, so that's the exact
 * seam we mock here — no Ollama process needs to be running for these
 * tests to pass, and they run in milliseconds every time.
 *
 * We do NOT separately unit test OllamaEmbeddingProviderClient's HTTP
 * behavior in this phase — verifying it actually talks to Ollama
 * correctly would require either a running Ollama instance or Spring's
 * MockRestServiceServer, which is real test infrastructure better
 * suited to a dedicated testing phase (same trade-off reasoning we used
 * for DocumentChunkRepository's derived query in Phase 2.4).
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingProviderClient embeddingProviderClient;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(embeddingProviderClient);
    }

    @Test
    void generateEmbedding_shouldReturnAFloatArrayForValidText() {
        when(embeddingProviderClient.embed("Amazon Web Services internship"))
                .thenReturn(List.of(0.123, -0.421, 0.087));

        float[] embedding = embeddingService.generateEmbedding("Amazon Web Services internship");

        assertThat(embedding).hasSize(3);
        assertThat(embedding[0]).isEqualTo(0.123f, org.assertj.core.data.Offset.offset(0.0001f));
        assertThat(embedding[1]).isEqualTo(-0.421f, org.assertj.core.data.Offset.offset(0.0001f));
        assertThat(embedding[2]).isEqualTo(0.087f, org.assertj.core.data.Offset.offset(0.0001f));
    }

    @Test
    void generateEmbedding_shouldSendTheExactTextToTheProvider() {
        when(embeddingProviderClient.embed("some chunk of document text"))
                .thenReturn(List.of(0.1, 0.2));

        embeddingService.generateEmbedding("some chunk of document text");

        // Confirms EmbeddingService passes the text straight through,
        // with no accidental trimming, casing changes, or mutation.
        verify(embeddingProviderClient).embed("some chunk of document text");
    }

    @Test
    void generateEmbedding_shouldThrowForNullInput() {
        assertThatThrownBy(() -> embeddingService.generateEmbedding(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void generateEmbedding_shouldThrowForBlankInput() {
        assertThatThrownBy(() -> embeddingService.generateEmbedding("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void generateEmbedding_shouldPropagateProviderFailuresWithoutSwallowingThem() {
        when(embeddingProviderClient.embed("some text"))
                .thenThrow(new EmbeddingGenerationException("Failed to reach the embedding provider"));

        assertThatThrownBy(() -> embeddingService.generateEmbedding("some text"))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("Failed to reach the embedding provider");
    }

    @Test
    void generateEmbedding_shouldPreserveVectorOrderAndDimensionCount() {
        // A slightly longer vector, to confirm we're not just handling
        // the trivial 2-3 element case.
        List<Double> rawVector = List.of(0.01, 0.02, 0.03, 0.04, 0.05);
        when(embeddingProviderClient.embed("longer text")).thenReturn(rawVector);

        float[] embedding = embeddingService.generateEmbedding("longer text");

        assertThat(embedding).hasSize(5);
        for (int i = 0; i < rawVector.size(); i++) {
            assertThat(embedding[i]).isEqualTo(rawVector.get(i).floatValue());
        }
    }
}
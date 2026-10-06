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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void generatesEmbeddingForValidText() {
        when(embeddingProviderClient.embed("hello world"))
                .thenReturn(List.of(0.1, -0.2, 0.3));

        float[] result = embeddingService.generateEmbedding("hello world");

        assertThat(result).containsExactly(0.1f, -0.2f, 0.3f);
        verify(embeddingProviderClient).embed("hello world");
    }

    @Test
    void sendsExactTextToProvider() {
        when(embeddingProviderClient.embed(anyString())).thenReturn(List.of(0.1));

        embeddingService.generateEmbedding("Amazon Web Services internship");

        verify(embeddingProviderClient).embed("Amazon Web Services internship");
    }

    @Test
    void throwsExceptionForNullText() {
        assertThatThrownBy(() -> embeddingService.generateEmbedding(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text must not be null");
    }

    @Test
    void throwsExceptionForBlankText() {
        assertThatThrownBy(() -> embeddingService.generateEmbedding("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text must not be blank");
    }

    @Test
    void wrapsProviderFailureInEmbeddingGenerationException() {
        when(embeddingProviderClient.embed(anyString()))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> embeddingService.generateEmbedding("some text"))
                .isInstanceOf(EmbeddingGenerationException.class);
    }

    @Test
    void propagatesEmbeddingGenerationExceptionFromProviderWithoutDoubleWrapping() {
        EmbeddingGenerationException original = new EmbeddingGenerationException("provider down");
        when(embeddingProviderClient.embed(anyString())).thenThrow(original);

        assertThatThrownBy(() -> embeddingService.generateEmbedding("some text"))
                .isSameAs(original);
    }

    @Test
    void throwsExceptionWhenProviderReturnsEmptyList() {
        when(embeddingProviderClient.embed(anyString())).thenReturn(List.of());

        assertThatThrownBy(() -> embeddingService.generateEmbedding("some text"))
                .isInstanceOf(EmbeddingGenerationException.class);
    }

    @Test
    void throwsExceptionWhenProviderReturnsNull() {
        when(embeddingProviderClient.embed(anyString())).thenReturn(null);

        assertThatThrownBy(() -> embeddingService.generateEmbedding("some text"))
                .isInstanceOf(EmbeddingGenerationException.class);
    }
}
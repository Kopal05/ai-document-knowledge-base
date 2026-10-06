package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.SearchResponse;
import com.kopal.smartknowledgebase.exception.DocumentNotFoundException;
import com.kopal.smartknowledgebase.exception.InvalidSearchQueryException;
import com.kopal.smartknowledgebase.repository.DocumentChunkRepository;
import com.kopal.smartknowledgebase.repository.DocumentChunkSearchProjection;
import com.kopal.smartknowledgebase.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    private static final Long DOCUMENT_ID = 1L;
    private static final Long OWNER_ID = 100L;
    private static final int DEFAULT_TOP_K = 5;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkSearchProjection projection;

    private SemanticSearchService semanticSearchService;

    @BeforeEach
    void setUp() {
        semanticSearchService = new SemanticSearchService(
                embeddingService, documentChunkRepository, documentRepository, DEFAULT_TOP_K);
    }

    @Test
    void returnsMappedResultsForValidQuery() {
        when(documentRepository.existsByIdAndOwnerId(DOCUMENT_ID, OWNER_ID)).thenReturn(true);
        when(embeddingService.generateEmbedding("JWT auth")).thenReturn(new float[]{0.1f, 0.2f});
        when(projection.getId()).thenReturn(17L);
        when(projection.getDocumentId()).thenReturn(DOCUMENT_ID);
        when(projection.getChunkIndex()).thenReturn(3);
        when(projection.getChunkText()).thenReturn("JWT works by...");
        when(projection.getSimilarity()).thenReturn(0.91);
        when(documentChunkRepository.searchSimilarChunks(eq(DOCUMENT_ID), anyString(), eq(DEFAULT_TOP_K)))
                .thenReturn(List.of(projection));

        SearchResponse response = semanticSearchService.search(DOCUMENT_ID, "JWT auth", null, OWNER_ID);

        assertThat(response.getQuery()).isEqualTo("JWT auth");
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getChunkId()).isEqualTo(17L);
        assertThat(response.getResults().get(0).getSimilarity()).isEqualTo(0.91);
    }

    @Test
    void generatesQueryEmbeddingUsingEmbeddingService() {
        when(documentRepository.existsByIdAndOwnerId(DOCUMENT_ID, OWNER_ID)).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(documentChunkRepository.searchSimilarChunks(anyLong(), anyString(), anyInt()))
                .thenReturn(List.of());

        semanticSearchService.search(DOCUMENT_ID, "How does JWT work?", null, OWNER_ID);

        verify(embeddingService).generateEmbedding("How does JWT work?");
    }

    @Test
    void usesProvidedTopKWhenPositive() {
        when(documentRepository.existsByIdAndOwnerId(DOCUMENT_ID, OWNER_ID)).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(documentChunkRepository.searchSimilarChunks(anyLong(), anyString(), anyInt()))
                .thenReturn(List.of());

        semanticSearchService.search(DOCUMENT_ID, "query", 10, OWNER_ID);

        verify(documentChunkRepository).searchSimilarChunks(eq(DOCUMENT_ID), anyString(), eq(10));
    }

    @Test
    void fallsBackToDefaultTopKWhenNullOrNonPositive() {
        when(documentRepository.existsByIdAndOwnerId(DOCUMENT_ID, OWNER_ID)).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(documentChunkRepository.searchSimilarChunks(anyLong(), anyString(), anyInt()))
                .thenReturn(List.of());

        semanticSearchService.search(DOCUMENT_ID, "query", 0, OWNER_ID);

        verify(documentChunkRepository).searchSimilarChunks(eq(DOCUMENT_ID), anyString(), eq(DEFAULT_TOP_K));
    }

    @Test
    void throwsExceptionWhenQueryIsBlank() {
        assertThatThrownBy(() -> semanticSearchService.search(DOCUMENT_ID, "   ", null, OWNER_ID))
                .isInstanceOf(InvalidSearchQueryException.class);
    }

    @Test
    void throwsExceptionWhenQueryIsNull() {
        assertThatThrownBy(() -> semanticSearchService.search(DOCUMENT_ID, null, null, OWNER_ID))
                .isInstanceOf(InvalidSearchQueryException.class);
    }

    @Test
    void throwsDocumentNotFoundWhenDocumentDoesNotBelongToOwner() {
        when(documentRepository.existsByIdAndOwnerId(DOCUMENT_ID, OWNER_ID)).thenReturn(false);

        assertThatThrownBy(() -> semanticSearchService.search(DOCUMENT_ID, "query", null, OWNER_ID))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void returnsEmptyResultsWhenNoChunksMatch() {
        when(documentRepository.existsByIdAndOwnerId(DOCUMENT_ID, OWNER_ID)).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(documentChunkRepository.searchSimilarChunks(anyLong(), anyString(), anyInt()))
                .thenReturn(List.of());

        SearchResponse response = semanticSearchService.search(DOCUMENT_ID, "query", null, OWNER_ID);

        assertThat(response.getResults()).isEmpty();
    }

    @Test
    void propagatesEmbeddingGenerationFailure() {
        when(documentRepository.existsByIdAndOwnerId(DOCUMENT_ID, OWNER_ID)).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString()))
                .thenThrow(new com.kopal.smartknowledgebase.exception.EmbeddingGenerationException("down"));

        assertThatThrownBy(() -> semanticSearchService.search(DOCUMENT_ID, "query", null, OWNER_ID))
                .isInstanceOf(com.kopal.smartknowledgebase.exception.EmbeddingGenerationException.class);
    }
}
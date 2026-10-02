package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.SearchResponse;
import com.kopal.smartknowledgebase.exception.DocumentNotFoundException;
import com.kopal.smartknowledgebase.exception.EmbeddingGenerationException;
import com.kopal.smartknowledgebase.exception.InvalidSearchQueryException;
import com.kopal.smartknowledgebase.repository.DocumentChunkRepository;
import com.kopal.smartknowledgebase.repository.DocumentChunkSearchProjection;
import com.kopal.smartknowledgebase.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SemanticSearchService — no Spring context, no database.
 * DocumentChunkSearchProjection is a plain interface, so Mockito mocks it
 * directly (mock(DocumentChunkSearchProjection.class)) exactly like any
 * other collaborator — no real native query ever runs here.
 *
 * WHY THERE'S NO TEST THAT ACTUALLY EXECUTES THE NATIVE SQL:
 * Proving the pgvector query itself is correct requires a real
 * PostgreSQL connection with the vector extension enabled — the same
 * trade-off already made for DocumentChunkRepository's earlier derived
 * query (Phase 2.4) and for DocumentChunk's column mapping (Phase 3.2).
 * What's unit-testable and IS tested here is everything
 * SemanticSearchService itself is responsible for: validation, the
 * default-topK decision, and correctly wiring EmbeddingService's output
 * into the repository call and the repository's output into the
 * response DTO.
 */
@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    private static final int CONFIGURED_DEFAULT_TOP_K = 5;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentRepository documentRepository;

    private SemanticSearchService semanticSearchService;

    @BeforeEach
    void setUp() {
        semanticSearchService = new SemanticSearchService(
                embeddingService, documentChunkRepository, documentRepository, CONFIGURED_DEFAULT_TOP_K);
    }

    @Test
    void search_shouldReturnMappedResultsForAValidQuery() {
        when(documentRepository.existsById(1L)).thenReturn(true);
        when(embeddingService.generateEmbedding("How does JWT work?"))
                .thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        DocumentChunkSearchProjection row = mockProjection(17L, 1L, 3, "JWT chunk text", 0.91);
        when(documentChunkRepository.searchSimilarChunks(eq(1L), anyString(), anyInt()))
                .thenReturn(List.of(row));

        SearchResponse response = semanticSearchService.search(1L, "How does JWT work?", 5);

        assertThat(response.getQuery()).isEqualTo("How does JWT work?");
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getChunkId()).isEqualTo(17L);
        assertThat(response.getResults().get(0).getDocumentId()).isEqualTo(1L);
        assertThat(response.getResults().get(0).getChunkIndex()).isEqualTo(3);
        assertThat(response.getResults().get(0).getChunkText()).isEqualTo("JWT chunk text");
        assertThat(response.getResults().get(0).getSimilarity()).isEqualTo(0.91);
    }

    @Test
    void search_shouldGenerateTheQueryEmbeddingUsingTheExistingEmbeddingService() {
        when(documentRepository.existsById(1L)).thenReturn(true);
        when(embeddingService.generateEmbedding("some query")).thenReturn(new float[]{0.1f});
        when(documentChunkRepository.searchSimilarChunks(any(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        semanticSearchService.search(1L, "some query", 5);

        verify(embeddingService).generateEmbedding("some query");
    }

    @Test
    void search_shouldCallRepositoryWithDocumentIdAndTopK() {
        when(documentRepository.existsById(1L)).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(documentChunkRepository.searchSimilarChunks(any(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        semanticSearchService.search(1L, "some query", 3);

        ArgumentCaptor<String> vectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(documentChunkRepository).searchSimilarChunks(eq(1L), vectorCaptor.capture(), eq(3));

        // Confirms the vector was actually formatted as a pgvector
        // literal, without hardcoding exact float-to-string formatting.
        String vectorLiteral = vectorCaptor.getValue();
        assertThat(vectorLiteral).startsWith("[").endsWith("]");
        assertThat(vectorLiteral.split(",")).hasSize(2);
    }

    @Test
    void search_shouldFallBackToConfiguredDefaultTopKWhenNullIsGiven() {
        when(documentRepository.existsById(1L)).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(documentChunkRepository.searchSimilarChunks(any(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        semanticSearchService.search(1L, "some query", null);

        verify(documentChunkRepository).searchSimilarChunks(eq(1L), anyString(), eq(CONFIGURED_DEFAULT_TOP_K));
    }

    @Test
    void search_shouldThrowDocumentNotFoundWhenDocumentDoesNotExist() {
        when(documentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> semanticSearchService.search(99L, "some query", 5))
                .isInstanceOf(DocumentNotFoundException.class);

        // Confirms we fail BEFORE calling the embedding provider at
        // all — no wasted Ollama call for a document that doesn't exist.
        verify(embeddingService, never()).generateEmbedding(anyString());
    }

    @Test
    void search_shouldThrowForNullOrBlankQuery() {
        assertThatThrownBy(() -> semanticSearchService.search(1L, null, 5))
                .isInstanceOf(InvalidSearchQueryException.class);

        assertThatThrownBy(() -> semanticSearchService.search(1L, "   ", 5))
                .isInstanceOf(InvalidSearchQueryException.class);
    }

    @Test
    void search_shouldReturnEmptyResultsWhenNoChunksMatchRatherThanThrowing() {
        when(documentRepository.existsById(1L)).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(documentChunkRepository.searchSimilarChunks(any(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        SearchResponse response = semanticSearchService.search(1L, "no matches for this", 5);

        assertThat(response.getResults()).isEmpty();
    }

    @Test
    void search_shouldPropagateEmbeddingGenerationFailures() {
        when(documentRepository.existsById(1L)).thenReturn(true);
        when(embeddingService.generateEmbedding(anyString()))
                .thenThrow(new EmbeddingGenerationException("Failed to reach the embedding provider"));

        assertThatThrownBy(() -> semanticSearchService.search(1L, "some query", 5))
                .isInstanceOf(EmbeddingGenerationException.class);

        verify(documentChunkRepository, never()).searchSimilarChunks(any(), anyString(), anyInt());
    }

    private DocumentChunkSearchProjection mockProjection(Long id, Long documentId, Integer chunkIndex,
                                                         String chunkText, Double similarity) {
        DocumentChunkSearchProjection projection = mock(DocumentChunkSearchProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getDocumentId()).thenReturn(documentId);
        when(projection.getChunkIndex()).thenReturn(chunkIndex);
        when(projection.getChunkText()).thenReturn(chunkText);
        when(projection.getSimilarity()).thenReturn(similarity);
        return projection;
    }
}
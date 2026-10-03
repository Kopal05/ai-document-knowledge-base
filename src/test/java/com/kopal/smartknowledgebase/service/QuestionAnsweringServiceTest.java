package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.AskResponse;
import com.kopal.smartknowledgebase.dto.SearchResponse;
import com.kopal.smartknowledgebase.dto.SearchResult;
import com.kopal.smartknowledgebase.exception.AnswerGenerationException;
import com.kopal.smartknowledgebase.exception.DocumentNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for QuestionAnsweringService — no Spring context, no
 * database, no real Ollama call. SemanticSearchService and
 * ChatCompletionClient are both mocked, so this is purely testing
 * QuestionAnsweringService's OWN orchestration logic: does it call
 * SemanticSearchService with the right arguments, does it build a
 * sensible prompt, does it skip the LLM call when there's no context,
 * and does it let failures from either collaborator propagate
 * unchanged.
 *
 * WHY THERE'S NO SEPARATE OllamaChatCompletionClientTest:
 * Same trade-off already made for OllamaEmbeddingProviderClient back in
 * Phase 3.1: verifying its actual HTTP behavior against Ollama's wire
 * format would need either a running Ollama instance or Spring's
 * MockRestServiceServer — real test infrastructure better suited to a
 * dedicated testing phase, not duplicated ad hoc here.
 */
@ExtendWith(MockitoExtension.class)
class QuestionAnsweringServiceTest {

    private static final int CONFIGURED_CONTEXT_CHUNK_COUNT = 5;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private ChatCompletionClient chatCompletionClient;

    private QuestionAnsweringService questionAnsweringService;

    @BeforeEach
    void setUp() {
        questionAnsweringService = new QuestionAnsweringService(
                semanticSearchService, chatCompletionClient, CONFIGURED_CONTEXT_CHUNK_COUNT);
    }

    @Test
    void ask_shouldReturnGeneratedAnswerWithSourcesForAValidQuestion() {
        SearchResult source = new SearchResult(17L, 1L, 3, "I worked on JWT auth at Amazon.", 0.91);
        when(semanticSearchService.search(1L, "What was my role at Amazon?", CONFIGURED_CONTEXT_CHUNK_COUNT))
                .thenReturn(new SearchResponse("What was my role at Amazon?", List.of(source)));

        when(chatCompletionClient.generateAnswer(anyString()))
                .thenReturn("You worked on JWT authentication.");

        AskResponse response = questionAnsweringService.ask(1L, "What was my role at Amazon?");

        assertThat(response.getQuestion()).isEqualTo("What was my role at Amazon?");
        assertThat(response.getAnswer()).isEqualTo("You worked on JWT authentication.");
        assertThat(response.getSources()).containsExactly(source);
    }

    @Test
    void ask_shouldReuseSemanticSearchServiceWithTheConfiguredChunkCount() {
        when(semanticSearchService.search(any(), anyString(), any()))
                .thenReturn(new SearchResponse("some question", List.of(
                        new SearchResult(1L, 1L, 0, "text", 0.8))));
        when(chatCompletionClient.generateAnswer(anyString())).thenReturn("an answer");

        questionAnsweringService.ask(1L, "some question");

        verify(semanticSearchService).search(1L, "some question", CONFIGURED_CONTEXT_CHUNK_COUNT);
    }

    @Test
    void ask_shouldIncludeRetrievedChunkTextAndTheQuestionInThePrompt() {
        when(semanticSearchService.search(any(), anyString(), any()))
                .thenReturn(new SearchResponse("How does JWT work?", List.of(
                        new SearchResult(1L, 1L, 0, "JWT is a token-based auth mechanism.", 0.9))));
        when(chatCompletionClient.generateAnswer(anyString())).thenReturn("an answer");

        questionAnsweringService.ask(1L, "How does JWT work?");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatCompletionClient).generateAnswer(promptCaptor.capture());

        String prompt = promptCaptor.getValue();
        assertThat(prompt)
                .contains("JWT is a token-based auth mechanism.")
                .contains("How does JWT work?");
    }

    @Test
    void ask_shouldReturnNoInformationAnswerWithoutCallingTheLlmWhenNoChunksAreFound() {
        when(semanticSearchService.search(any(), anyString(), any()))
                .thenReturn(new SearchResponse("unrelated question", Collections.emptyList()));

        AskResponse response = questionAnsweringService.ask(1L, "unrelated question");

        assertThat(response.getAnswer())
                .isEqualTo("I don't have enough information in this document to answer that question.");
        assertThat(response.getSources()).isEmpty();

        // The whole point of this path: no wasted LLM call when there's
        // nothing to ground an answer in.
        verify(chatCompletionClient, never()).generateAnswer(anyString());
    }

    @Test
    void ask_shouldPropagateDocumentNotFoundFromSemanticSearchServiceUnchanged() {
        when(semanticSearchService.search(eq(99L), anyString(), any()))
                .thenThrow(new DocumentNotFoundException(99L));

        assertThatThrownBy(() -> questionAnsweringService.ask(99L, "some question"))
                .isInstanceOf(DocumentNotFoundException.class);

        verify(chatCompletionClient, never()).generateAnswer(anyString());
    }

    @Test
    void ask_shouldPropagateChatProviderFailures() {
        when(semanticSearchService.search(any(), anyString(), any()))
                .thenReturn(new SearchResponse("some question", List.of(
                        new SearchResult(1L, 1L, 0, "some text", 0.8))));
        when(chatCompletionClient.generateAnswer(anyString()))
                .thenThrow(new AnswerGenerationException("Failed to reach the chat provider"));

        assertThatThrownBy(() -> questionAnsweringService.ask(1L, "some question"))
                .isInstanceOf(AnswerGenerationException.class);
    }
}
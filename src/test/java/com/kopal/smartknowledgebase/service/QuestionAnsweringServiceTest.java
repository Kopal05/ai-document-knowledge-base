package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.AskResponse;
import com.kopal.smartknowledgebase.dto.SearchResponse;
import com.kopal.smartknowledgebase.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionAnsweringServiceTest {

    private static final Long DOCUMENT_ID = 1L;
    private static final Long OWNER_ID = 100L;
    private static final int CONTEXT_CHUNK_COUNT = 5;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private ChatCompletionClient chatCompletionClient;

    private QuestionAnsweringService questionAnsweringService;

    @BeforeEach
    void setUp() {
        questionAnsweringService = new QuestionAnsweringService(
                semanticSearchService, chatCompletionClient, CONTEXT_CHUNK_COUNT);
    }

    @Test
    void returnsAnswerWithSourcesWhenChunksFound() {
        SearchResult source = new SearchResult(1L, DOCUMENT_ID, 0, "JWT is a token format.", 0.9);
        when(semanticSearchService.search(DOCUMENT_ID, "What is JWT?", CONTEXT_CHUNK_COUNT, OWNER_ID))
                .thenReturn(new SearchResponse("What is JWT?", List.of(source)));
        when(chatCompletionClient.generateAnswer(anyString()))
                .thenReturn("  JWT stands for JSON Web Token.  ");

        AskResponse response = questionAnsweringService.ask(DOCUMENT_ID, "What is JWT?", OWNER_ID);

        assertThat(response.getAnswer()).isEqualTo("JWT stands for JSON Web Token.");
        assertThat(response.getSources()).containsExactly(source);
    }

    @Test
    void returnsNoInformationAnswerWithoutCallingLlmWhenNoChunksFound() {
        when(semanticSearchService.search(DOCUMENT_ID, "unrelated question", CONTEXT_CHUNK_COUNT, OWNER_ID))
                .thenReturn(new SearchResponse("unrelated question", List.of()));

        AskResponse response = questionAnsweringService.ask(DOCUMENT_ID, "unrelated question", OWNER_ID);

        assertThat(response.getAnswer())
                .isEqualTo("I don't have enough information in this document to answer that question.");
        assertThat(response.getSources()).isEmpty();
        verify(chatCompletionClient, never()).generateAnswer(anyString());
    }

    @Test
    void passesOwnerIdThroughToSemanticSearch() {
        when(semanticSearchService.search(anyLong(), anyString(), anyInt(), anyLong()))
                .thenReturn(new SearchResponse("q", List.of()));

        questionAnsweringService.ask(DOCUMENT_ID, "q", OWNER_ID);

        verify(semanticSearchService).search(DOCUMENT_ID, "q", CONTEXT_CHUNK_COUNT, OWNER_ID);
    }

    @Test
    void includesSourceChunkTextInPromptSentToLlm() {
        SearchResult source = new SearchResult(1L, DOCUMENT_ID, 0, "unique context sentence", 0.9);
        when(semanticSearchService.search(anyLong(), anyString(), anyInt(), anyLong()))
                .thenReturn(new SearchResponse("q", List.of(source)));
        when(chatCompletionClient.generateAnswer(anyString())).thenReturn("answer");

        questionAnsweringService.ask(DOCUMENT_ID, "q", OWNER_ID);

        org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(chatCompletionClient).generateAnswer(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("unique context sentence");
    }
}
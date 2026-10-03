package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.exception.AnswerGenerationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Talks to a locally-running Ollama server's /api/generate endpoint to
 * produce a text answer for a given prompt.
 *
 * WHY @Component (NOT @Service) — same reasoning as
 * OllamaEmbeddingProviderClient: this is an adapter to a specific
 * external system (infrastructure), not core business logic.
 *
 * WHY "stream": false:
 * Ollama's /api/generate streams partial-token JSON lines by default —
 * fine for a typing-effect UI, useless for a method that needs to return
 * one complete String. Setting stream=false makes Ollama do the
 * buffering itself and return ONE JSON object containing the full
 * "response" field — verified against current Ollama docs rather than
 * assumed, since getting a wire-format detail like this wrong would
 * silently break every answer.
 *
 * WHY /api/generate, NOT /api/chat:
 * /api/chat is built for multi-turn conversations (an array of role-
 * tagged messages, conversation history). This phase is single-turn —
 * one question, one answer, no memory of previous questions — so
 * /api/generate's simpler "one prompt string in, one text out" shape is
 * the right fit. Nothing here would need to change at the
 * QuestionAnsweringService or controller level if a future phase added
 * conversation memory and swapped this class to use /api/chat instead —
 * that's exactly the point of depending on the ChatCompletionClient
 * interface rather than this class directly.
 */
@Component
public class OllamaChatCompletionClient implements ChatCompletionClient {

    private final RestClient chatRestClient;
    private final String model;

    public OllamaChatCompletionClient(RestClient chatRestClient,
                                      @Value("${chat.provider.model}") String model) {
        this.chatRestClient = chatRestClient;
        this.model = model;
    }

    @Override
    public String generateAnswer(String prompt) {
        GenerateRequest requestBody = new GenerateRequest(model, prompt, false);

        GenerateResponse response;
        try {
            response = chatRestClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(GenerateResponse.class);
        } catch (RestClientException e) {
            throw new AnswerGenerationException(
                    "Failed to reach the chat provider (model: " + model + ")", e);
        }

        if (response == null || response.response() == null || response.response().isBlank()) {
            throw new AnswerGenerationException("Chat provider returned an empty response");
        }

        return response.response();
    }

    // Wire-format records, specific to Ollama's /api/generate shape —
    // deliberately NOT in the shared dto package, same reasoning as
    // OllamaEmbeddingProviderClient's EmbedRequest/EmbedResponse.
    private record GenerateRequest(String model, String prompt, boolean stream) {
    }

    private record GenerateResponse(String response) {
    }
}
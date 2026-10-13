package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.exception.AnswerGenerationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Talks to Google's Gemini API to generate RAG answers, selected only
 * when the "prod" profile is active.
 *
 * WIRE FORMAT — verified against Google's current REST API docs
 * (ai.google.dev/api/generate-content), not assumed:
 * POST /v1beta/models/{model}:generateContent
 * Request:  {"contents": [{"parts": [{"text": "..."}]}]}
 * Response: {"candidates": [{"content": {"parts": [{"text": "..."}]}}]}
 *
 * This is single-turn (one prompt in, one answer out) — same shape as
 * OllamaChatCompletionClient's use of /api/generate, no conversation
 * history, consistent with QuestionAnsweringService building one
 * self-contained grounded prompt per call.
 */
@Component
@Profile("prod")
public class GeminiChatCompletionClient implements ChatCompletionClient {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    private final RestClient geminiChatRestClient;
    private final String model;

    public GeminiChatCompletionClient(RestClient geminiChatRestClient,
                                      @Value("${gemini.chat.model:gemini-flash-latest}") String model) {
        this.geminiChatRestClient = geminiChatRestClient;
        this.model = model;
    }

    @Override
    public String generateAnswer(String prompt) {
        GenerateContentRequest requestBody =
                new GenerateContentRequest(List.of(new Content(List.of(new Part(prompt)))));

        GenerateContentResponse response = callWithRetry(requestBody);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new AnswerGenerationException("Gemini chat provider returned no candidates");
        }

        Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()
                || content.parts().get(0).text() == null || content.parts().get(0).text().isBlank()) {
            throw new AnswerGenerationException("Gemini chat provider returned an empty response");
        }

        return content.parts().get(0).text();
    }

    /**
     * Calls Gemini's generateContent endpoint, retrying a limited number
     * of times with a short backoff when Gemini reports 503 (model
     * temporarily overloaded) — this is a routine, transient condition
     * with Gemini's API, not a bug in this client, so a few quick retries
     * resolve most occurrences without surfacing an error to the caller.
     * Any other failure (network error, 4xx, timeout) fails immediately,
     * same as before.
     */
    private GenerateContentResponse callWithRetry(GenerateContentRequest requestBody) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return geminiChatRestClient.post()
                        .uri("/v1beta/models/{model}:generateContent", model)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(GenerateContentResponse.class);
            } catch (HttpServerErrorException.ServiceUnavailable e) {
                if (attempt == MAX_RETRIES) {

                    throw new AnswerGenerationException(
                            "Gemini chat provider is currently overloaded (model: " + model
                                    + ") after " + MAX_RETRIES + " attempts", e);
                }
                sleep(BASE_DELAY_MS * attempt);
            } catch (RestClientException e) {
                // Covers connection failures, timeouts, and other non-2xx
                // responses (e.g. auth/quota errors) — same boundary
                // AnswerGenerationException already defines for
                // OllamaChatCompletionClient. Not retried: these aren't
                // transient capacity issues.
                throw new AnswerGenerationException(
                        "Failed to reach the Gemini chat provider (model: " + model + ")", e);
            }
        }
        // Unreachable: loop either returns or throws.
        throw new AnswerGenerationException("Failed to reach the Gemini chat provider (model: " + model + ")");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AnswerGenerationException("Interrupted while retrying Gemini chat call");
        }
    }

    // Wire-format records specific to Gemini's generateContent shape —
    // deliberately not in the shared dto package, same reasoning as
    // OllamaChatCompletionClient's GenerateRequest/GenerateResponse.
    private record GenerateContentRequest(List<Content> contents) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record GenerateContentResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }
}
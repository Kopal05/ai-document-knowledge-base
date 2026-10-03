package com.kopal.smartknowledgebase.exception;

/**
 * Thrown when generating an answer fails — the chat/LLM provider
 * couldn't be reached, returned an error, or returned something
 * unusable.
 *
 * WHY THIS MIRRORS EmbeddingGenerationException EXACTLY:
 * Same role, different provider: OllamaChatCompletionClient catches the
 * low-level failure (network error, bad HTTP status, empty response) and
 * rethrows it as this one, unchecked, domain-specific exception, so
 * QuestionAnsweringService and the controller never need to know or
 * handle Spring's RestClientException type directly.
 */
public class AnswerGenerationException extends RuntimeException {

    public AnswerGenerationException(String message) {
        super(message);
    }

    public AnswerGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
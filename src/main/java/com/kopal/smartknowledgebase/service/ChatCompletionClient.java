package com.kopal.smartknowledgebase.service;

/**
 * Abstraction over "some external system that turns a prompt into
 * generated text." QuestionAnsweringService depends on THIS interface,
 * not on any concrete HTTP client or provider SDK — structurally
 * identical to EmbeddingProviderClient, and for the same two reasons:
 * swapping chat providers later means writing one new implementation
 * class with zero changes to QuestionAnsweringService, and testing
 * QuestionAnsweringService means mocking this one simple method instead
 * of mocking RestClient's fluent chain.
 */
public interface ChatCompletionClient {

    String generateAnswer(String prompt);
}
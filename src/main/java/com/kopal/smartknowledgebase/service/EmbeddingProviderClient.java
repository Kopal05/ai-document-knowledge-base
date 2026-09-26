package com.kopal.smartknowledgebase.service;

import java.util.List;

/**
 * Abstraction over "some external system that turns text into a vector
 * of numbers." EmbeddingService depends on THIS interface, not on any
 * concrete HTTP client or provider SDK.
 *
 * WHY THIS INTERFACE EXISTS:
 * Today there is exactly one implementation, OllamaEmbeddingProviderClient.
 * The interface still earns its place for two concrete reasons:
 *  1. Swapping providers later (e.g. moving to a paid cloud API once this
 *     is deployed) means writing ONE new class that implements this
 *     interface — EmbeddingService itself never changes.
 *  2. Testing EmbeddingService means mocking this one simple method
 *     instead of mocking Spring's RestClient, which has a long fluent
 *     method chain (.post().uri().contentType().body().retrieve().body())
 *     that's awkward and brittle to mock directly.
 *
 * This returns List<Double> (not float[]) deliberately: this is the raw,
 * unprocessed wire-format result from the provider. EmbeddingService is
 * the one place that decides to convert it into float[] — keeping that
 * decision in one place, not duplicated into every provider
 * implementation we might add later.
 */
public interface EmbeddingProviderClient {

    List<Double> embed(String text);
}
package com.kopal.smartknowledgebase.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A deliberately tiny test.
 *
 * WHY THIS IS THE ONLY TEST FOR PHASE 3.2:
 * The change this phase makes is a schema mapping, not business logic —
 * there's no algorithm here to exercise with inputs and outputs the way
 * TextChunkingServiceTest or EmbeddingServiceTest do. Proving the actual
 * database column is correct requires a real PostgreSQL connection (see
 * the SQL verification steps instead) — introducing Testcontainers just
 * to prove one annotation combination works would be exactly the kind of
 * complexity this phase was told to avoid.
 *
 * What IS worth guarding with a real (if small) test: DocumentChunk.
 * EMBEDDING_DIMENSIONS is a "magic number that must stay correct" —
 * if someone changes it without realizing it's tied to nomic-embed-text's
 * actual output size, every future embedding insert would fail with a
 * dimension mismatch. This test is cheap, fast, and makes that
 * assumption explicit and checked.
 */
class DocumentChunkTest {

    @Test
    void embeddingDimensions_shouldMatchNomicEmbedTextOutputSize() {
        // If this ever needs to change, it must change together with
        // whichever embedding model EmbeddingService/OllamaEmbeddingProviderClient
        // actually uses — see application.properties' embedding.provider.model.
        assertThat(DocumentChunk.EMBEDDING_DIMENSIONS).isEqualTo(768);
    }
}
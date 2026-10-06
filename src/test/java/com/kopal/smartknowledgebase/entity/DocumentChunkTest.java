package com.kopal.smartknowledgebase.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkTest {

    @Test
    void embeddingDimensionsConstantMatchesNomicEmbedTextModel() {
        assertThat(DocumentChunk.EMBEDDING_DIMENSIONS).isEqualTo(768);
    }

    @Test
    void embeddingFieldAcceptsArrayMatchingDimensionsConstant() {
        DocumentChunk chunk = new DocumentChunk();
        float[] embedding = new float[DocumentChunk.EMBEDDING_DIMENSIONS];

        chunk.setEmbedding(embedding);

        assertThat(chunk.getEmbedding()).hasSize(768);
    }
}
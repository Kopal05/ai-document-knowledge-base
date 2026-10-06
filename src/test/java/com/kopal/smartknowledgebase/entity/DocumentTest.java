package com.kopal.smartknowledgebase.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTest {

    @Test
    void addChunkSetsBothSidesOfRelationship() {
        Document document = new Document();
        DocumentChunk chunk = new DocumentChunk();

        document.addChunk(chunk);

        assertThat(document.getChunks()).containsExactly(chunk);
        assertThat(chunk.getDocument()).isEqualTo(document);
    }

    @Test
    void addingMultipleChunksPreservesOrderOfInsertion() {
        Document document = new Document();
        DocumentChunk chunk0 = new DocumentChunk();
        chunk0.setChunkIndex(0);
        DocumentChunk chunk1 = new DocumentChunk();
        chunk1.setChunkIndex(1);

        document.addChunk(chunk0);
        document.addChunk(chunk1);

        assertThat(document.getChunks()).containsExactly(chunk0, chunk1);
    }
}
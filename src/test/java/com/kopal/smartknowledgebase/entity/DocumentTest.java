package com.kopal.smartknowledgebase.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the Document <-> DocumentChunk relationship itself — entirely in
 * memory, no Spring context, no database. This is possible because a
 * bidirectional JPA relationship is, underneath the annotations, just
 * plain Java object references: Document holds a List<DocumentChunk>,
 * and each DocumentChunk holds a reference back to its Document. We can
 * verify that wiring is correct without ever touching Hibernate or
 * PostgreSQL.
 *
 * NOTE: DocumentService's actual upload flow does NOT use
 * Document.addChunk() (it builds DocumentChunks directly and saves them
 * via DocumentChunkRepository — see DocumentService for why). This test
 * exists to verify addChunk() itself behaves correctly for whichever
 * future code path does rely on it (tests, or a later "append more
 * chunks to an existing document" feature).
 */
class DocumentTest {

    @Test
    void addChunk_shouldSetBothSidesOfTheRelationship() {
        Document document = new Document();
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("some text");
        chunk.setChunkIndex(0);

        document.addChunk(chunk);

        assertThat(document.getChunks()).containsExactly(chunk);
        assertThat(chunk.getDocument()).isSameAs(document);
    }

    @Test
    void addChunk_shouldPreserveInsertionOrder() {
        Document document = new Document();

        DocumentChunk first = new DocumentChunk();
        first.setChunkIndex(0);
        first.setChunkText("first");

        DocumentChunk second = new DocumentChunk();
        second.setChunkIndex(1);
        second.setChunkText("second");

        DocumentChunk third = new DocumentChunk();
        third.setChunkIndex(2);
        third.setChunkText("third");

        document.addChunk(first);
        document.addChunk(second);
        document.addChunk(third);

        assertThat(document.getChunks())
                .extracting(DocumentChunk::getChunkText)
                .containsExactly("first", "second", "third");

        assertThat(document.getChunks())
                .extracting(DocumentChunk::getChunkIndex)
                .containsExactly(0, 1, 2);
    }

    @Test
    void newDocument_shouldStartWithAnEmptyChunksListNotNull() {
        Document document = new Document();

        // This matters in practice: if `chunks` defaulted to null instead
        // of an empty list, calling addChunk() on a brand-new Document
        // would throw a NullPointerException instead of just working.
        assertThat(document.getChunks()).isNotNull().isEmpty();
    }
}
package com.kopal.smartknowledgebase.repository;

import com.kopal.smartknowledgebase.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for the DocumentChunk entity.
 *
 * WHY THIS FILE EXISTS:
 * Same reasoning as DocumentRepository — this is where "talking to the
 * document_chunks table" lives, kept separate from DocumentRepository
 * because DocumentChunk is its own entity with its own table.
 *
 * findByDocumentIdOrderByChunkIndexAsc IS THE IMPLEMENTATION:
 * This is a Spring Data JPA "derived query" — we write no SQL and no
 * method body at all. Spring Data parses the METHOD NAME itself at
 * startup and generates the query from it:
 *   findBy                -> SELECT ... WHERE
 *   DocumentId             -> document.id = ?1  (note: DocumentChunk has
 *                              no "documentId" field — Spring Data walks
 *                              the "document" association and reads ITS
 *                              "id" field automatically)
 *   OrderByChunkIndexAsc   -> ORDER BY chunk_index ASC
 * The equivalent SQL is roughly:
 *   SELECT * FROM document_chunks
 *   WHERE document_id = ?
 *   ORDER BY chunk_index ASC
 *
 * We're not calling this method yet in this phase (DocumentService
 * doesn't need to READ chunks back out yet), but it's here now because
 * it's central to how future phases (embeddings, retrieval) will fetch a
 * document's chunks in their original, correct order.
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);
}
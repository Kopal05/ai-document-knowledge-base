package com.kopal.smartknowledgebase.repository;

import com.kopal.smartknowledgebase.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Finds the topK chunks, within one document, most semantically
     * similar to the given query vector — using pgvector's cosine
     * distance operator (<=>), computed entirely inside PostgreSQL.
     *
     * WHY NATIVE SQL (nativeQuery = true):
     * pgvector's distance operators don't exist in JPQL/HQL's query
     * language — there is no derived-method-name or JPQL equivalent of
     * "<=>". Falling back to a native query is the documented, correct
     * approach for exactly this situation; everything database-specific
     * about this stays isolated here, in the repository layer — nothing
     * above this (SemanticSearchService, the controller) knows or needs
     * to know this particular query isn't a "normal" one.
     *
     * WHY queryVector IS A String, NOT A float[]:
     * Binding a raw float[] as an ad-hoc native query PARAMETER (as
     * opposed to a mapped ENTITY FIELD, which hibernate-vector already
     * handles correctly via @JdbcTypeCode) isn't reliably documented for
     * this Spring Data JPA version. Instead, the caller
     * (SemanticSearchService) converts the embedding into pgvector's own
     * plain-text literal format ("[0.12,-0.04,...]"), which is passed as
     * an ordinary String parameter — completely standard JDBC binding,
     * zero special type registration needed. CAST(:queryVector AS
     * vector) turns that string into a real vector value exactly where
     * PostgreSQL needs it.
     *
     * WHY embedding IS NOT NULL:
     * Chunks created before embedding generation existed (or any future
     * chunk whose embedding generation somehow didn't happen) have
     * embedding = NULL. The <=> operator has no meaningful result
     * against NULL, so these are excluded rather than producing garbage
     * or errors.
     *
     * WHY ORDER BY THE RAW DISTANCE EXPRESSION (not the similarity alias):
     * They're mathematically equivalent (similarity = 1 - distance), but
     * ordering by the raw "embedding <=> vector" expression is exactly
     * the query shape a future HNSW/IVFFlat index is built to accelerate
     * — worth forming the query this way now even without an index yet.
     */
    @Query(value = """
            SELECT c.id AS id,
                   c.document_id AS documentId,
                   c.chunk_index AS chunkIndex,
                   c.chunk_text AS chunkText,
                   1 - (c.embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM document_chunks c
            WHERE c.document_id = :documentId
              AND c.embedding IS NOT NULL
            ORDER BY c.embedding <=> CAST(:queryVector AS vector) ASC
            LIMIT :topK
            """, nativeQuery = true)
    List<DocumentChunkSearchProjection> searchSimilarChunks(
            @Param("documentId") Long documentId,
            @Param("queryVector") String queryVector,
            @Param("topK") int topK);
}
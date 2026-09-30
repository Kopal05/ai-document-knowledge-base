package com.kopal.smartknowledgebase.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * JPA entity representing one chunk of a Document's extracted text, in
 * the "document_chunks" table.
 *
 * WHY THIS FILE EXISTS:
 * A Document can have many chunks (produced by TextChunkingService), and
 * each chunk needs to be its own row so it can be individually retrieved,
 * ordered, and — in a later phase — linked to its own embedding vector.
 *
 * WHAT YOU SHOULD UNDERSTAND BEFORE MODIFYING THIS:
 * - @ManyToOne + @JoinColumn(name = "document_id") makes DocumentChunk the
 *   OWNING side of the Document <-> DocumentChunk relationship. "Owning
 *   side" just means: this is the entity whose table actually has the
 *   foreign key column. In a one-to-many/many-to-one pair, the "many"
 *   side is always the owner, because that's where the FK naturally
 *   lives — a document_chunks row can only ever point at ONE document,
 *   so document_id belongs on THIS table, not on documents.
 * - fetch = FetchType.LAZY means Hibernate does NOT automatically load
 *   the parent Document every time a DocumentChunk is loaded — it only
 *   fetches it if you actually call chunk.getDocument(). This avoids
 *   accidentally pulling in a full Document (and, via ITS collection,
 *   every other chunk) just because you looked at one chunk.
 * - chunkText is stored as TEXT (not the default VARCHAR(255)) because a
 *   500-character chunk (our current default chunk size) would already
 *   overflow a default varchar column, and PostgreSQL's TEXT type has no
 *   fixed length limit.
 * - There is no @AllArgsConstructor here (unlike Document) because we
 *   always build a DocumentChunk with the no-args constructor + setters
 *   in DocumentService — a multi-argument constructor would just be
 *   unused complexity for now.
 * - This entity is never returned directly by the controller either;
 *   once we build endpoints that expose chunks, they'll get their own
 *   DTO, same reasoning as Document/DocumentResponse.
 *
 * NEW IN PHASE 3.2 — the embedding field:
 * - EMBEDDING_DIMENSIONS = 768 because that's exactly what our chosen
 *   embedding model, nomic-embed-text (see EmbeddingService), produces.
 *   This is a compile-time constant (not read from
 *   application.properties) because @Array's `length` is an annotation
 *   attribute, and annotation attribute values must be compile-time
 *   constants — you can't hand an annotation a value read from a config
 *   file at runtime. It's also conceptually tied to the SCHEMA (how many
 *   numbers this column can hold), not to something that should differ
 *   between environments the way a URL would. If the embedding model
 *   ever changes to one with a different output size, this constant AND
 *   the actual database column both have to change together.
 * - @JdbcTypeCode(SqlTypes.VECTOR) + @Array(length = EMBEDDING_DIMENSIONS)
 *   is Hibernate ORM's own, officially supported way (Hibernate 6.4+, via
 *   the separate hibernate-vector module — see pom.xml) of mapping a
 *   Java float[] to a PostgreSQL pgvector `vector(N)` column. This is
 *   deliberately NOT the older, manual approach of forcing
 *   columnDefinition = "vector(768)" plus @JdbcTypeCode(SqlTypes.OTHER):
 *   that older approach can get ddl-auto=update to create the right
 *   *column type*, but it does NOT teach Hibernate how to correctly
 *   serialize an actual populated float[] into pgvector's wire format —
 *   it would likely only work for inserting NULL. hibernate-vector's
 *   SqlTypes.VECTOR handles both the DDL generation AND the real
 *   read/write binding correctly, which matters even though we aren't
 *   writing real vectors until Phase 3.3 — we want this column mapping
 *   to be genuinely correct now, not just "correct enough to look right
 *   in \d document_chunks."
 * - nullable is left at its default (true) DELIBERATELY: nothing
 *   currently sets this field (DocumentService's upload flow doesn't
 *   touch it yet), so every chunk created before Phase 3.3 will have
 *   embedding = NULL. If this were NOT NULL, the existing, currently-
 *   working PDF upload flow would start failing immediately.
 * - This field is NOT populated from Ollama in this phase — see
 *   EmbeddingService, which remains completely disconnected from this
 *   entity for now, exactly as designed in Phase 3.1.
 */
@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
public class DocumentChunk {

    public static final int EMBEDDING_DIMENSIONS = 768;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = EMBEDDING_DIMENSIONS)
    @Column(name = "embedding")
    private float[] embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
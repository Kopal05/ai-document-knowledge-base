package com.kopal.smartknowledgebase.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
 */
@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
public class DocumentChunk {

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
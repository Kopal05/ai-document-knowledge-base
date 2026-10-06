package com.kopal.smartknowledgebase.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a document row in the "documents" table.
 *
 * WHY THIS FILE EXISTS:
 * An entity is a plain Java class mapped to a database table by Hibernate
 * (the JPA implementation Spring Boot uses under the hood). Each field
 * becomes a column, each instance becomes a row.
 *
 * WHAT YOU SHOULD UNDERSTAND BEFORE MODIFYING THIS:
 * - @Entity tells Hibernate "manage this class as a database table".
 * - @Id marks the primary key field.
 * - @GeneratedValue(strategy = GenerationType.IDENTITY) tells Postgres to
 *   auto-increment the id using a native identity column (BIGSERIAL-like
 *   behavior). This is a simple, PostgreSQL-friendly strategy for a
 *   learning project. (SEQUENCE is another common option you may explore
 *   later, which tends to batch inserts more efficiently.)
 * - This entity is NEVER returned directly from the controller. Instead we
 *   convert it to/from DTOs (see the dto package). This keeps our public
 *   API contract independent from our database schema — we can change a
 *   column name here without breaking API consumers, and we avoid
 *   accidentally leaking internal fields.
 * - createdAt/updatedAt are set manually in the lifecycle callbacks below
 *   (@PrePersist / @PreUpdate) instead of relying on extra libraries, so
 *   you can see exactly when/how they're populated.
 *
 * NEW IN THIS PHASE — the chunks relationship:
 * - @OneToMany(mappedBy = "document", ...) is the INVERSE (non-owning)
 *   side of the relationship. mappedBy = "document" tells Hibernate
 *   "don't create a foreign key column on THIS table — go look at the
 *   field named 'document' over on DocumentChunk, that's where the real
 *   foreign key lives." Document itself gets no new column at all from
 *   this relationship.
 * - cascade = CascadeType.ALL + orphanRemoval = true together mean: if a
 *   Document is deleted, every DocumentChunk that still points at it is
 *   deleted too, and if a chunk is ever removed from this list, it's
 *   deleted from the database (not just unlinked). This is appropriate
 *   because a chunk has no meaning without its parent document — it's a
 *   composition relationship, not just an association.
 * - Note this cascade governs DELETE behavior for this project.
 *   DocumentService does NOT rely on cascade to INSERT chunks — it saves
 *   them explicitly through DocumentChunkRepository instead (see
 *   DocumentService for why).
 * - chunks is initialized to `new ArrayList<>()` so addChunk(...) can be
 *   called safely on a brand-new, not-yet-saved Document without a
 *   NullPointerException.
 *
 * NEW — the owner relationship:
 * - @ManyToOne + @JoinColumn(name = "owner_id") — same owning-side
 *   pattern as DocumentChunk.document: the "many" side (many Documents
 *   per User) holds the foreign key. nullable = false means every
 *   document MUST have an owner from creation — there's no "orphan"
 *   document state to account for anywhere else in the code.
 * - fetch = FetchType.LAZY for the same reason as DocumentChunk.document:
 *   loading a Document shouldn't automatically pull in its owning User
 *   unless something actually asks for it.
 * - No cascade here (unlike the chunks relationship): deleting a User
 *   deleting all their Documents isn't a feature this phase implements
 *   (there's no "delete my account" endpoint at all yet) — leaving
 *   cascade unset avoids silently defining delete behavior for a
 *   scenario that doesn't exist yet.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunk> chunks = new ArrayList<>();

    public void addChunk(DocumentChunk chunk) {
        chunks.add(chunk);
        chunk.setDocument(this);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
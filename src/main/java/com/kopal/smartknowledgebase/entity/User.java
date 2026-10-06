package com.kopal.smartknowledgebase.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity representing a registered user, in the "users" table.
 *
 * WHY THIS CLASS DOES NOT IMPLEMENT Spring Security's UserDetails:
 * Same reasoning Document/DocumentChunk already follow throughout this
 * project: an entity's only job is to be a database row. UserDetails is
 * a Spring Security CONCERN (authorities, account-expiry flags, etc.)
 * that has nothing to do with "what a user is" as a domain concept — it
 * only matters to the authentication machinery. Mixing it into this
 * class would mean every future change to how Spring Security represents
 * a principal forces a change to this entity too. See
 * security/CustomUserDetails for the actual UserDetails implementation —
 * a thin wrapper around this entity, built only where Spring Security
 * needs one.
 *
 * WHY passwordHash, NOT password:
 * The field name itself documents that a plaintext password must never
 * be assigned here — only the output of PasswordEncoder.encode(...).
 * The column is similarly named password_hash for the same reason: if
 * someone inspects the documents table directly, the column name itself
 * is a reminder of what's (and isn't) stored there.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
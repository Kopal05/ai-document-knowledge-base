package com.kopal.smartknowledgebase.repository;

import com.kopal.smartknowledgebase.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for User. Both methods are derived queries (same
 * pattern as findByDocumentIdOrderByChunkIndexAsc in
 * DocumentChunkRepository) — Spring Data generates the implementation
 * from the method name, no SQL or method body written here.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
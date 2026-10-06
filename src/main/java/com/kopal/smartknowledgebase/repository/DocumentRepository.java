package com.kopal.smartknowledgebase.repository;

import com.kopal.smartknowledgebase.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for the Document entity.
 *
 * WHY THIS FILE EXISTS:
 * This interface is where "talking to the database" lives. We don't write
 * any SQL or implementation here — extending JpaRepository<Document, Long>
 * gives us, for free, methods like:
 *   save(), findById(), findAll(), deleteById(), existsById(), count() ...
 *
 * Spring Data JPA generates the implementation of this interface at
 * runtime (a dynamic proxy) and registers it as a bean, which is why we
 * can simply @Autowired / constructor-inject it into DocumentService
 * without writing a class that implements it ourselves.
 *
 * @Repository is technically optional here (Spring Data detects
 * JpaRepository subinterfaces automatically), but keeping it makes the
 * component's role explicit and enables Spring's automatic translation of
 * database exceptions into Spring's DataAccessException hierarchy.
 *
 * OWNERSHIP-SCOPED METHODS (added for authentication/authorization):
 * Every document-related read/write that a controller exposes now goes
 * through one of findByIdAndOwnerId / existsByIdAndOwnerId /
 * findAllByOwnerId instead of the plain findById/existsById/findAll
 * JpaRepository already provides. This is the actual enforcement point
 * for "users can only access their own documents" — DocumentService
 * never trusts a caller-supplied ownerId from a request body; it only
 * ever receives one already-derived from the validated JWT (see
 * DocumentController's @AuthenticationPrincipal usage).
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    List<Document> findAllByOwnerId(Long ownerId);
}
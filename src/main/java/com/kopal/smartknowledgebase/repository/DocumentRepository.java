package com.kopal.smartknowledgebase.repository;

import com.kopal.smartknowledgebase.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
 * We are intentionally NOT adding custom query methods
 * (e.g. findByTitleContaining) yet — the task only needs basic CRUD.
 * Add custom queries only when a real feature needs them.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
}

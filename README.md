# Smart Document Q&A / Knowledge Base API

## 1. What this project is

A backend API that will eventually let users upload PDF documents and ask
natural-language questions about their content, using RAG (Retrieval
Augmented Generation) with vector embeddings and an LLM.

This repository currently contains **Phase 1 only**: a clean, layered
Spring Boot skeleton with basic CRUD for a `Document` resource, backed by
PostgreSQL. Everything else (PDF processing, embeddings, auth, caching,
deployment, etc.) is intentionally **not implemented yet** — it's being
built incrementally as a learning exercise.

## 2. Current features (Phase 1)

- Create / list / fetch / delete a `Document` record (metadata only — no
  actual file upload yet)
- Layered architecture: Controller → Service → Repository → PostgreSQL
- Request validation with Bean Validation
- Centralized error handling with `@RestControllerAdvice`
- One example unit test for the service layer

## 3. Tech stack

- Java 21
- Spring Boot 3.3 (Spring Web, Spring Data JPA)
- Hibernate
- PostgreSQL
- Maven
- Lombok
- JUnit 5 + Mockito + AssertJ (testing)

## 4. Project structure

```
com.kopal.smartknowledgebase
├── controller       # HTTP endpoints, thin, no business logic
├── service           # Business logic, entity <-> DTO mapping
├── repository        # Spring Data JPA interfaces
├── entity             # JPA-mapped database entities
├── dto                # Request/response shapes exposed by the API
├── exception          # Custom exceptions + global exception handler
├── config             # (empty for now) future @Configuration classes
└── SmartKnowledgeBaseApplication.java
```

## 5. How to run locally

### Prerequisites
- Java 21 (JDK)
- Maven (or use the included wrapper if you add one)
- A running local PostgreSQL instance

### Steps
1. Clone/open the project in IntelliJ.
2. Set up the database (see section 6 below).
3. Either export environment variables, or just rely on the defaults in
   `application.properties` (username `postgres`, password `postgres`,
   database `smart_knowledge_base`, host `localhost:5432`):
   ```
   export DB_URL=jdbc:postgresql://localhost:5432/smart_knowledge_base
   export DB_USERNAME=postgres
   export DB_PASSWORD=your_password
   ```
4. Run the application:
   ```
   mvn spring-boot:run
   ```
   or run `SmartKnowledgeBaseApplication` directly from IntelliJ.
5. The API will be available at `http://localhost:8080`.

## 6. PostgreSQL setup

1. Install PostgreSQL locally (or run it in a container — Docker comes
   later in the roadmap, so for now a local install is simplest).
2. Create the database:
   ```sql
   CREATE DATABASE smart_knowledge_base;
   ```
3. Make sure a user/role exists with access to it (the default `postgres`
   superuser works fine for local development).
4. That's it — with `spring.jpa.hibernate.ddl-auto=update`, Hibernate will
   create the `documents` table automatically the first time you run the
   app.

## 7. Current API endpoints

| Method | Path                  | Description              |
|--------|-----------------------|---------------------------|
| POST   | `/api/documents`      | Create a document         |
| GET    | `/api/documents`      | List all documents        |
| GET    | `/api/documents/{id}` | Get a document by id      |
| DELETE | `/api/documents/{id}` | Delete a document by id   |

Example request body for `POST /api/documents`:
```json
{
  "title": "My Document",
  "fileName": "document.pdf"
}
```

## 8. Future planned features (NOT IMPLEMENTED YET)

- PDF upload + text extraction
- Document chunking
- Embedding generation
- pgvector storage + semantic similarity search
- RAG pipeline + LLM-generated answers
- JWT authentication & authorization
- Redis caching
- Rate limiting
- Swagger/OpenAPI docs
- Full unit + integration test suite (Testcontainers)
- Docker
- GitHub Actions CI/CD
- AWS deployment

See the "Future Roadmap" section shared alongside this README for the
phase-by-phase plan.

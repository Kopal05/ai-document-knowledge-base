# AI-Powered Document Knowledge Base & Q&A System

A backend project where users upload PDF documents and search their
content using meaning rather than exact keyword matches. The current
milestone supports PDF text extraction, chunking, embeddings, pgvector
storage, and semantic search. RAG answer generation, a frontend, and
deployment are planned next.

## Project Checklist

### Foundation

-   [x] Create a Java 21 / Spring Boot 3.3.4 Maven application.
-   [x] Organize code into Controller, Service, Repository, Entity, DTO,
    and exception layers.
-   [x] Connect PostgreSQL 17 using Spring Data JPA and Hibernate.
-   [x] Implement document CRUD functionality.
-   [x] Add request validation and centralized exception handling.
-   [x] Verify the API and database connection.

### PDF Processing

-   [x] Add Apache PDFBox for PDF text extraction.
-   [x] Implement `POST /api/documents/upload`.
-   [x] Validate uploaded files and handle extraction errors.
-   [x] Return document ID, filename, extracted text, and chunk count.
-   [x] Split extracted text into overlapping chunks.
-   [x] Add unit tests for text chunking.
-   [x] Save documents and their chunks in PostgreSQL.
-   [x] Verify stored chunks in the database.
-   [x] Configure the upload-size limit.

### Embeddings and Semantic Search

-   [x] Install Ollama and download `nomic-embed-text`.
-   [x] Verify that the model creates 768-dimensional embeddings.
-   [x] Implement an embedding provider client and embedding service.
-   [x] Add embedding error handling and mocked tests.
-   [x] Install pgvector and enable the `vector` extension in the
    project database.
-   [x] Map the embedding column as `vector(768)` using Hibernate.
-   [x] Generate and store embeddings for document chunks.
-   [x] Convert a search question into an embedding.
-   [x] Compare embeddings with pgvector to find semantically similar
    chunks.
-   [x] Return ranked matching chunks with similarity scores.
-   [x] Implement `GET /api/documents/{documentId}/search?query=...`.
-   [x] Test semantic search in Postman with resume-related questions.

### Planned

-   [ ] Add RAG so an LLM can generate natural-language answers using
    retrieved chunks.
-   [ ] Add source references to answers.
-   [ ] Add authentication and authorization.
-   [ ] Build a simple, polished, responsive frontend.
-   [ ] Deploy the frontend, backend, and database.
-   [ ] Add production improvements, CI/CD, and final documentation.

## Main Components

  -----------------------------------------------------------------------
  Component                           What it does
  ----------------------------------- -----------------------------------
  Java                                Programming language used for the
                                      backend.

  Spring Boot                         Helps build the REST API and
                                      application.

  Controller                          Receives HTTP requests and returns
                                      responses.

  Service                             Contains logic such as PDF
                                      processing and searching.

  Repository                          Reads and writes data through
                                      Spring Data JPA.

  JPA / Hibernate                     Maps Java objects to database
                                      records.

  PostgreSQL                          Stores document details and text
                                      chunks.

  pgvector                            Adds vector storage and similarity
                                      search to PostgreSQL.

  Apache PDFBox                       Extracts text from PDF files.

  Ollama `nomic-embed-text`           Runs locally to create
                                      768-dimensional text embeddings.

  Embedding                           A numerical representation of text
                                      meaning.

  Semantic search                     Finds related content even when
                                      exact words differ.

  RAG (planned)                       Gives retrieved passages to an LLM
                                      to help generate an answer.
  -----------------------------------------------------------------------

## Technology Stack

-   **Language:** Java 21
-   **Backend:** Spring Boot 3.3.4
-   **Build:** Maven
-   **Database:** PostgreSQL 17
-   **Persistence:** Spring Data JPA, Hibernate
-   **Vector search:** pgvector
-   **PDF processing:** Apache PDFBox
-   **Embeddings:** Ollama, `nomic-embed-text` (768 dimensions)
-   **Testing:** JUnit 5, Mockito, AssertJ
-   **API testing:** Postman
-   **Version control:** Git, GitHub

## Run Locally

### Requirements

-   Java 21 and Maven
-   PostgreSQL 17 with pgvector installed
-   Ollama with `nomic-embed-text`
-   Git

### Steps

1.  Clone the repository:

    ``` bash
    git clone https://github.com/Kopal05/ai-document-knowledge-base.git
    cd ai-document-knowledge-base
    ```

2.  Create the database if needed:

    ``` sql
    CREATE DATABASE smart_knowledge_base;
    ```

3.  Connect to `smart_knowledge_base` and enable pgvector:

    ``` sql
    CREATE EXTENSION IF NOT EXISTS vector;
    ```

4.  Make sure Ollama is running and the model is installed:

    ``` bash
    ollama pull nomic-embed-text
    ollama list
    ```

5.  Configure database credentials and other settings in
    `application.properties` or environment variables. Do not commit
    passwords or secrets. Refer to the repository's current
    configuration for the exact property names.

6.  Run tests:

    ``` bash
    mvn test
    ```

7.  Start the backend:

    ``` bash
    mvn spring-boot:run
    ```

The current local API uses `http://localhost:8080`.

## Current Status

**Completed:** Spring Boot and PostgreSQL foundation, PDF processing,
chunk persistence, Ollama embeddings, pgvector integration, and semantic
search tested locally.

**Next phase:** RAG-based answer generation. A simple frontend and
deployment are planned after the core backend flow is complete.

# AI-Powered Document Knowledge Base & Q&A System

**Live app:** https://ai-document-knowledge-base-cxyi.vercel.app
**API docs (Swagger):** https://ai-document-knowledge-base.onrender.com/swagger-ui/index.html

Both run on free hosting tiers, so the first request after a few minutes
of inactivity can take 30-90 seconds to wake up. That's expected.

## The idea

This started as a way to properly understand, not just use, the pieces
that go into a modern backend system — authentication, relational data
modeling, file processing, vector search, and retrieval-augmented
generation — by building a real project around them one phase at a time
instead of following a single end-to-end tutorial. Each phase below was
built, tested, and understood on its own before the next one was added,
which is also why the commit history reads less like a finished product
and more like a log of decisions.

The project itself: let someone upload a PDF and then ask it questions
in plain English, getting back an answer grounded in the document's
actual content rather than a model's general knowledge.

## What's been built

- User accounts with JWT-based authentication; every document belongs
  to the user who uploaded it, and nobody else can see or query it.
- PDF upload, text extraction, and chunking, with the chunks and their
  embeddings persisted in Postgres.
- Semantic search over a document's chunks using pgvector.
- Retrieval-augmented question answering: the same search, followed by
  an LLM call that answers only from the retrieved context.
- A production deployment with a separate database and AI provider from
  local development — not just a backend that runs on a laptop.
- A small React frontend so the whole thing is usable without going
  through Swagger or Postman.

## What it does

1. You register and log in. The API issues a JWT, and it's required on
   every request after that.
2. You upload a PDF. The backend extracts its text with Apache PDFBox,
   splits it into overlapping chunks, generates an embedding for each
   chunk, and stores both the text and the vector.
3. You either **search** a document — plain semantic retrieval, no AI
   generation, just the most relevant chunks and their similarity
   scores — or **ask** it a question, which runs that same retrieval
   and then hands the results to an LLM to produce an actual answer,
   shown alongside the chunks it came from.

## How a question actually gets answered

This is the part of the project the rest of it exists to support, so
it's worth walking through in full. The following happens on every
`ask` request, and it's the same code path in both local development
and production — only the embedding and chat providers underneath it
change.

1. The question is turned into a vector embedding.
2. That vector is compared against every chunk's stored embedding for
   the chosen document, using pgvector's cosine-distance operator
   directly inside Postgres. This comparison happens in the database,
   not in application code.
3. The most similar chunks come back, each with a similarity score.
4. If nothing relevant enough was found, the backend returns a fixed
   "not enough information" answer and skips the LLM call entirely —
   on purpose, so the model is never given an empty context and a
   chance to fill it with something invented.
5. Otherwise, the retrieved chunks are assembled into a prompt that
   instructs the model to answer strictly from that context, and to
   say plainly when the context doesn't contain the answer.
6. The model's response is returned together with the exact chunks it
   was grounded in, so the answer can be checked against its source
   rather than taken on faith.

## Tech stack

**Backend** — Java 21, Spring Boot 3.3.4, Maven, Spring Data JPA /
Hibernate, Spring Security with JWT (BCrypt-hashed passwords, stateless
sessions), Apache PDFBox, springdoc for the Swagger docs.

**Database and search** — PostgreSQL with the pgvector extension.
Flyway manages schema migrations in production; local development uses
Hibernate's auto-update instead, since the schema is still changing
often enough there that a formal migration per change would slow things
down for no real benefit yet.

**AI providers** — Ollama (`nomic-embed-text` for embeddings,
`llama3.2` for chat) locally; Google's Gemini API
(`gemini-embedding-001` and `gemini-flash-latest`) in production.

**Frontend** — React, TypeScript, Vite, plain CSS.

**Infrastructure** — Docker, Render (backend), Neon (managed Postgres),
Vercel (frontend).

**Testing** — JUnit 5, Mockito, AssertJ.

## Architecture

```
 Browser (React)
       │
       │ HTTPS, JWT in Authorization header
       ▼
 Spring Boot API ── Spring Security / JWT filter
       │
       ├── DocumentService ──── Apache PDFBox (extract + chunk)
       │
       ├── EmbeddingService ─── Ollama (local) or Gemini (prod)
       │
       ├── SemanticSearchService ── pgvector similarity search
       │
       └── QuestionAnsweringService ── chat completion
                 (reuses SemanticSearchService, then calls
                  Ollama or Gemini to generate the answer)
       │
       ▼
 PostgreSQL + pgvector
 (Neon in production, local Postgres in development)
```

Two things about this shape are worth calling out. First, the embedding
and chat providers sit behind interfaces
(`EmbeddingProviderClient`, `ChatCompletionClient`), each with a local
(Ollama) and a production (Gemini) implementation selected by Spring
profile — the services that use them never know which one is active.
Second, `QuestionAnsweringService` doesn't duplicate any retrieval
logic; it calls `SemanticSearchService` directly and only adds the
prompt-building and generation step on top, so there's exactly one
place that knows how to find relevant chunks.

## Running it locally

### Requirements

- Java 21 and Maven
- Node.js
- PostgreSQL with pgvector installed
- Ollama, with `nomic-embed-text` and `llama3.2` pulled
- Git

### Backend

```bash
git clone https://github.com/Kopal05/ai-document-knowledge-base.git
cd ai-document-knowledge-base
```

Create the database and enable pgvector:

```sql
CREATE DATABASE smart_knowledge_base;
\c smart_knowledge_base
CREATE EXTENSION IF NOT EXISTS vector;
```

Make sure Ollama is running with both models pulled:

```bash
ollama pull nomic-embed-text
ollama pull llama3.2
```

Set the required environment variables — `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, and `JWT_SECRET` (generate one with
`openssl rand -base64 32`) at minimum. See `.env.example` for the full
list.

```bash
mvn spring-boot:run
```

The API is now at `http://localhost:8080`, with interactive docs at
`http://localhost:8080/swagger-ui/index.html`.

### Frontend

```bash
cd frontend
npm install
```

Create a `.env` file in `frontend/`:

```
VITE_API_BASE_URL=http://localhost:8080
```

```bash
npm run dev
```

The app opens at `http://localhost:5173`. If requests to the backend
fail, check that `CORS_ALLOWED_ORIGIN` on the backend matches this URL
exactly.

## Scope

This is a learning project, not a product, and the scope reflects that
on purpose. In: everything described above, built and actually
deployed, not just working on a laptop. Out, for now: CI/CD, rate
limiting, structured logging and monitoring, multi-file or
multi-format document support (PDF only), and conversation history
across questions (every `ask` is independent — there's no memory of
earlier questions in the same session).

None of these are things I didn't think of; they're things I chose to
leave out so the core flow — upload, embed, retrieve, generate — could
be built and understood properly first, instead of spreading the same
amount of effort thinner across more features.

## Feedback

If something here is unclear, wrong, or could be done better, I'd
genuinely like to hear it — open an issue on this repo, or reach me
directly. Still actively working on this, so nothing here is final.
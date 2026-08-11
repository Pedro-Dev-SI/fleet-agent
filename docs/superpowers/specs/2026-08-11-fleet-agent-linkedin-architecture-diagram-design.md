# Fleet Agent LinkedIn Architecture Diagram Design

## Objective

Create a polished 16:9 architecture diagram in Eraser titled “Fleet Agent — AI Agent Architecture with Java and LangChain4j”. The diagram must explain how the AI agent participates in a real Java backend while preserving deterministic business rules in Java.

The visual is intended for a LinkedIn post and must remain legible on mobile. The vehicle-rental domain is supporting context, not the primary subject.

## Verified Architecture

The diagram reflects the current repository implementation:

- Spring Boot 3.5.16 and Java 25.
- A single Spring Boot modular monolith using Spring Modulith.
- LangChain4j with configurable Ollama or Gemini chat models.
- Chat memory keyed by `sessionId`, retaining the latest 10 messages.
- Input and output guardrails.
- RAG through a LangChain4j `RetrievalAugmentor`.
- SHA-256 document versioning, recursive chunks of 300 characters with overlap 30, `nomic-embed-text`, and pgvector embeddings with dimension 768.
- Semantic retrieval limited to three chunks with minimum score 0.65, including source, title, and category metadata.
- Tool calling through Assistant, Customer, and Reservation tools.
- Tools depend on public Customer and Rental module APIs rather than repositories.
- Reservation creation receives the conversation identifier through `@ToolMemoryId`.
- Reservation events are consumed by Notification after the database transaction commits.
- PostgreSQL 17 with pgvector, relational domain tables, knowledge documents, vector embeddings, and an IVFFlat cosine index.

## Composition

Use one large system boundary labeled “Fleet Agent — Spring Boot Modular Monolith”. No Java module is shown as an independently deployed service.

The primary left-to-right runtime story is:

`Client → POST /api/assistant → AI Service → Tool Calling → Java Business Logic → PostgreSQL → grounded response`

The AI Service is the largest and most prominent component. Chat Memory, Guardrails, and the selected Chat Model appear as compact capabilities directly associated with it.

The Knowledge Module sits below the AI Service and contains two concise lanes:

- Ingestion: Markdown → SHA-256 → recursive chunking 300/30 → `nomic-embed-text` → pgvector.
- Retrieval: user query → query embedding → cosine search → top 3 at score ≥ 0.65 → grounded context with source/title/category → AI Service.

Tool Calling sits to the right of the AI Service and contains:

- Assistant Tools: quotation and vehicle availability.
- Customer Tools: customer lookup and registration.
- Reservation Tools: create, review, and cancel, with an `@ToolMemoryId` annotation.

Java Business Logic contains Customer Module and Rental Module. Tool connections terminate at the appropriate public use cases. Rental consumes Customer’s public API. A small secondary Notification Module receives reservation-created and reservation-cancelled events after commit.

The database is one cylinder on the far right labeled “PostgreSQL 17 + pgvector”, divided visually into Domain Data and RAG Data.

## Visual Language

- Near-black or charcoal canvas with no white group backgrounds.
- Violet for AI and LLM concerns, with a restrained glow only around AI Service.
- Cyan for RAG and vector retrieval.
- Amber for Tool Calling.
- Green for deterministic Java/Spring modules.
- Blue for the PostgreSQL cylinder.
- Gray for supporting infrastructure and events.
- Rounded cards, thin directional arrows, consistent spacing, and modern sans-serif typography.
- Large labels and minimal copy suitable for LinkedIn mobile viewing.
- Avoid crossed connectors and unnecessary empty space.

## Required Message

The visual must make this distinction explicit:

> The LLM understands and orchestrates. Java validates and executes.

It must not introduce microservices, Kafka, Redis, external authentication, or any component absent from the repository.

## Validation

After creation, verify that:

1. The runtime path is understandable without reading every annotation.
2. All Java modules remain inside the single monolith boundary.
3. The RAG ingestion and retrieval paths are distinguishable.
4. Tools connect to public use cases, not repositories.
5. PostgreSQL uses cylinder notation and contains both domain and RAG sections.
6. The AI Service is visually dominant and all major labels remain readable at LinkedIn scale.

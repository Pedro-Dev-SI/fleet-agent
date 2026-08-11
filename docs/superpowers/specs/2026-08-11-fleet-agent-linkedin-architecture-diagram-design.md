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

The second supplied reference image defines the target density and hierarchy. The diagram uses a dark dotted canvas, a small number of large cards, short connectors, and generous spacing. No Java module is shown as an independently deployed service.

The primary story is:

`Client → POST /api/assistant → AI Service → Tool Calling → Java Business Logic → PostgreSQL → grounded response`

Use four clear zones with approximately eleven large cards total:

- Client: one prominent external actor card with the API route on its outgoing arrow.
- AI Service — LangChain4j: Chat Memory, Guardrails, and LLM — Ollama/Gemini.
- Tool Calling: Assistant Tools, Customer Tools, and Reservation Tools.
- Java Business Logic: Rental and Customer.

Place one compact Knowledge / RAG card beside the AI Service and one large PostgreSQL + pgvector card beside the RAG and Java zones.

Only relationships essential to the story appear:

- Client → AI Service: `POST /api/assistant`.
- AI Service → Tool Calling: `Selects tools`.
- AI Service ↔ Knowledge / RAG: `Retrieves context` and `Context + sources`.
- Assistant and Reservation Tools → Rental.
- Customer Tools → Customer.
- Rental and Customer → PostgreSQL.
- Knowledge / RAG ↔ PostgreSQL.

Omit ingestion steps, chunk sizes, similarity scores, table names, class names, method names, event names, and explanatory paragraphs from the canvas. These details belong in the LinkedIn caption or a second technical diagram. Omit Notification from this overview because it is secondary to the AI story.

Do not use a large outer system boundary when it creates empty space or reduces the scale of the cards. Communicate the modular monolith through the grouped Java modules and a concise subtitle instead.

## Visual Language

- Near-black dotted canvas matching the visual density of the second reference.
- Black or dark-gray cards with restrained colored accents.
- Green Spring icons for AI, tools, RAG, and Java modules; a PostgreSQL icon for the database.
- Rounded groups and large component cards.
- Primary card labels at 24 px or larger; secondary text is optional and never required to understand the diagram.
- Short orthogonal or gently curved connectors with labels of at most three words whenever possible.
- No crossed connectors, long perimeter arrows, tiny annotations, table lists, or large empty boundaries.

## Required Message

The visual must make this distinction explicit:

> The LLM understands and orchestrates. Java validates and executes.

It must not introduce microservices, Kafka, Redis, external authentication, or any component absent from the repository.

## Validation

After creation, verify that:

1. Client is immediately visible at normal zoom.
2. The complete runtime story is understandable from the component names and arrows alone.
3. The diagram contains approximately eleven cards and no detailed internal pipeline.
4. Tools connect to the correct Java modules.
5. No connector crosses another connector or runs around the entire canvas.
6. Every required label remains readable when the full 16:9 image fits on a 1200 px-wide screen.
7. The result is visually comparable in density and simplicity to the second supplied reference.

# Reservation Consistency Design

## Context

Fleet Agent already lets the AI create, review, and cancel reservations through stable Java contracts. The happy path works, but the current reservation implementation can return an arbitrary reservation for a customer, treat a cancelled reservation as an idempotent match, double-book a car under concurrent requests, and leave a car reserved forever after the rental end date.

This change hardens the `rental` business module without changing the AI prompt, tool signatures, controller contract, or conversation flow.

## Scope and business decisions

- A car uses an MVP global availability model: after reservation it remains `RESERVADO` until cancellation or automatic completion after `endDate`.
- A new reservation must start in the future and end after its start.
- Only `CREATED` and `CONFIRMED` reservations are active.
- Idempotency matches the same session, customer, car, start date, and end date, and only considers active reservations.
- A cancelled reservation does not prevent a new reservation in the same conversation.
- Repeating cancellation of an already cancelled reservation succeeds without publishing a second cancellation event.
- Customer review returns the most recent active reservation. The public response remains singular to preserve the AI contract.
- Expired active reservations transition to `COMPLETED` and release their cars through an internal scheduled job.

## Approaches considered

### 1. Global car lock with automatic completion — selected

The car status is the source of immediate availability. Reservation creation obtains a database write lock for the selected car, checks idempotency, and changes `DISPONIVEL` to `RESERVADO` in one transaction. A scheduled application service completes expired reservations.

This fits the existing UI/tool behavior, because the AI lists vehicles before collecting rental dates. It also has the smallest public and architectural impact.

### 2. Date-range availability

Availability would be calculated from overlapping reservation periods. This supports multiple future bookings for one car but conflicts with the current `list available cars by category` tool, which has no date parameters. Adopting it now would either make listing imprecise or require an AI contract change, so it is deferred.

### 3. Optimistic locking only

A JPA version field could detect concurrent updates, but one request would fail with a persistence conflict that still needs translation and retry handling. A pessimistic row lock is clearer for this short, high-value transaction and is selected for the MVP.

## Module placement and boundaries

All business changes stay inside `io.github.pedrodevsi.fleetagent.rental`.

- `rental.domain.Reservation` owns valid status transitions (`cancel` and `complete`).
- `rental.application.ReservationService` coordinates synchronous customer validation, locked car allocation, persistence, and domain event publication.
- A small internal `rental.application.ReservationExpirationService` completes expired reservations transactionally.
- A small internal scheduler triggers expiration; it is not exposed through `rental::api`.
- `rental.repository` owns locking and deterministic reservation queries.
- `customer::api` remains the only synchronous cross-module dependency.
- Creation and cancellation notifications remain event-driven through the existing public rental events.

The `ai` module continues to depend only on `rental::api`. No AI source file or public rental DTO changes.

## Creation flow

1. Validate required values and temporal rules using an injected `Clock`.
2. Resolve the customer synchronously through `customer::api`.
3. Lock the car row selected by model.
4. Search for an identical active reservation to handle tool retries.
5. If an identical active reservation exists, return it successfully without writes or another event.
6. If the car is not `DISPONIVEL`, return the existing unavailable response.
7. Mark it `RESERVADO`, save the reservation as `CREATED`, and publish one creation event.

A database partial unique index for active reservations provides a final invariant in addition to the transactional lock.

## Review and cancellation flow

Review resolves the customer and loads the active reservation ordered by start date descending. This avoids the non-unique-result failure currently possible with `Optional<Reservation> findByCustomerId(...)`.

Cancellation validates ownership before transition. Cancelling `CREATED` or `CONFIRMED` changes it to `CANCELLED`, releases the car, and publishes one event. Cancelling an already cancelled reservation returns success and performs no mutation or event publication. A completed reservation remains non-cancellable.

## Automatic completion

An internal scheduled component periodically asks the application service to complete active reservations whose `endDate` is not after the current time. The repository locks the selected reservations during processing. Each reservation transitions to `COMPLETED`, and its car becomes `DISPONIVEL`.

The schedule interval is externalized under `app.rental.reservation-completion-interval`, with a one-minute default. No LLM call, AI memory, prompt, or tool is involved.

## Persistence

A new Flyway migration will:

- add a check constraint enforcing `end_date > start_date`;
- add a unique constraint for `car.model`, because the current public contract identifies a vehicle by model;
- add a unique constraint for `car.plate`;
- add a partial unique index allowing at most one active reservation per car;
- add indexes supporting active customer review and expiration scanning.

Existing migration files will not be rewritten, preserving Flyway checksums.

## Error handling and observability

Expected business rejections continue to use the existing response DTOs so the AI receives the same response shape. Invalid dates, unavailable cars, missing customers, ownership mismatch, and invalid state transitions do not publish events.

The expiration job logs the number of completed reservations at `INFO` only when work occurs. Identifiers may be logged, but customer documents are not added to new log messages.

## TDD strategy

Implementation proceeds in red-green-refactor cycles:

1. Add failing unit tests for past dates, deterministic review, active-only idempotency, recreation after cancellation, idempotent repeated cancellation, and completion rules.
2. Add failing repository/integration coverage for ordering, active-state filtering, and write locking where the environment supports PostgreSQL Testcontainers.
3. Implement the minimum domain and repository changes to make each group green.
4. Add failing tests for the expiration service and scheduler delegation, then implement them.
5. Run the complete Maven suite and Spring Modulith `ApplicationModules.verify()` test.

## Acceptance criteria

- All existing AI tools and DTO signatures remain unchanged.
- Two active reservations cannot allocate the same car.
- An exact repeated create request returns the existing active reservation and emits no duplicate event.
- A cancelled reservation can be recreated in the same session.
- Repeated cancellation is successful and emits no duplicate event.
- Review never fails because a customer has multiple reservations and returns the most recent active one.
- Past or invalid date ranges are rejected without persistence or events.
- Expired active reservations become `COMPLETED` and their cars become `DISPONIVEL`.
- The full test suite and Spring Modulith verification pass on Java 25.

## Deferred work

- Date-range inventory and overlapping future reservations.
- Payment-driven confirmation.
- Pickup/check-out operations.
- Multiple-reservation responses through the AI.
- Distributed scheduling or message brokers.

# Spring Boot Redis Guardrail Microservice

A highly concurrent API gateway and guardrail system implementing strict mathematical constraints using Redis.

## Features & Implementation Guarantees

### Phase 1: Database & Endpoints
- Postgres serves as the source of truth for standard entities (`User`, `Bot`, `Post`, `Comment`). 
- Simple standard REST endpoints configured.

### Phase 2: Atomic Locks and Virality
- **Horizontal Cap Thread Safety:** To guarantee that exact limits (100 bots max per post) are met, the application leverages Redis's strictly deterministic `INCR` command (`opsForValue().increment()`). Because Redis evaluates strictly single-threaded, `INCR` provides purely atomic read-modify-write without race conditions. If the incremented value exceeds the threshold (100), the request is rejected *before* any db transactions execute.
- **Cooldown Cap Thread Safety:** Utilizing `SETNX` (`setIfAbsent()`) with a 10-minute TTL. This fundamentally prevents race condition overrides for the Bot-to-Human cooldown lock.

### Phase 3: Notifications
- Features a scheduled cron-task pulling mock summarized payloads via `opsForList()` batches and cleanly logging them, saving db overhead.

## Setup Instructions

1. **Start Services**
```bash
docker-compose up -d
```

2. **Start the App**
To compile and run:
```bash
mvn spring-boot:run
```

3. **Concurrency Test**
Run the Spring Boot integration test designed precisely to spam 200 bot requests concurrently.
```bash
mvn test -Dtest=ConcurrencySpamTest
```

## Postman Testing
Import `guardrail_postman_collection.json` directly into Postman to review payload structures.

## Known Limitations & Future Improvements

**Distributed Transactions ("Lost Slot" anomaly):**
Currently, the Redis limits (`checkHorizontalCap`) are verified and incremented before committing the changes to PostgreSQL. Because these are two separate systems, they do not share transactions. If the PostgreSQL transaction fails or rolls back (e.g. database goes down or a constraint issue), the Redis counter does not rollback. This leads to a persistent "Lost Slot" anomaly where a bot limit slot is permanently wasted. In a production system, this could be resolved gracefully by employing the Saga Pattern, Two-Phase Commits (2PC), or employing a worker queue / outbox pattern to finalize distributed system consistency.

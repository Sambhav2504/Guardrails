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

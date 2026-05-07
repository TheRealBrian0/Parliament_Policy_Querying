# Chroma DB Integration and Rolling Session Retention

This plan covers migrating the `InMemoryEmbeddingStore` to a persistent Chroma DB instance, and refining the session retention logic to automatically keep only the 3 most recent sessions while deleting older data.

## User Review Required

> [!WARNING]
> This requires adding a new service to `docker-compose.yml`. You will need to re-run `docker-compose up -d` after these changes are applied.

> [!IMPORTANT]
> The current system stores SQL data and embeddings separately. Deleting an old session requires cascading deletes from `SessionMetadataEntity` -> `SessionChunkEntity`, as well as deleting from the Chroma DB collection. 

## Open Questions

- Should we retain the `SessionChunkEntity` relational table if we are moving to Chroma DB, or should we continue to write to both (MySQL as source of truth, Chroma for vectors)? I recommend keeping MySQL as the source of truth for raw text since we might want to re-embed if we ever change embedding models in the future.
- Do we have any preferred Chroma DB version we should use in the `docker-compose.yml`? (I will default to `chromadb/chroma:latest` if not specified).

## Proposed Changes

---

### Infrastructure & Dependencies

#### [MODIFY] docker-compose.yml
- Add a new `chroma` service using the `chromadb/chroma:latest` image.
- Expose port `8000:8000`.
- Add a volume `chroma_data:/chroma/chroma` for persistence.

#### [MODIFY] pom.xml
- Add the `langchain4j-chroma` dependency to enable Chroma DB integration.

#### [MODIFY] application.yml
- Add a `chromadb.url: http://localhost:8000` property so the backend can connect to the vector store.

---

### Vector Store Service

#### [MODIFY] InMemoryVectorIndexService.java
- Rename to `VectorIndexService.java`.
- Change the implementation from `InMemoryEmbeddingStore` to `ChromaEmbeddingStore`.
- Remove the `@PostConstruct` logic that loads all chunks from the database on startup, because Chroma DB natively persists its embeddings on disk.
- Add a `deleteBySessionId(long sessionId)` method. Since LangChain4j's `ChromaEmbeddingStore` might not have a built-in `delete` by metadata method out of the box in the `0.35.0` version, we will implement a direct HTTP call (via Spring's `RestClient` or `RestTemplate`) to Chroma's `/api/v1/collections/{collection_name}/delete` endpoint using a `where: {"sessionId": "..."}` payload.

---

### Session Retention Logic

#### [MODIFY] SessionChunkRepository.java
- Add `void deleteBySessionId(long sessionId)` to allow wiping MySQL text chunks for old sessions.

#### [MODIFY] ChronologyService.java
- Replace `wipePreviousYearsIfCurrentYearStarted` with a new `enforceRollingSessionWindow` method.
- **Logic:**
  1. Fetch all `SessionMetadataEntity` ordered by `scrapeDate` descending.
  2. If the size is > 3, get the sublist of older sessions.
  3. For each old session:
     - Call `vectorIndexService.deleteBySessionId(session.getId())` to clear vectors from Chroma DB.
     - Call `sessionChunkRepository.deleteBySessionId(session.getId())` to clear raw text from MySQL.
     - Call `sessionMetadataRepository.delete(session)` to remove the metadata entry.

## Verification Plan

### Automated Tests
- Once changes are implemented, we will run `mvn clean compile` to ensure no syntax errors.

### Manual Verification
- Stop all containers and run `docker-compose up -d`.
- Verify the Chroma DB container is healthy.
- Run the Spring Boot application.
- Trigger a scrape or let the scheduler run, and verify that chunks are ingested into Chroma DB (can be checked via `curl http://localhost:8000/api/v1/collections`).
- Verify that older sessions (>3) are automatically purged from both MySQL and Chroma DB.

# Policy Pulse RAG - Project Context & Understanding

## 1. Project Overview
**Name**: `policy-pulse-rag` (Frontend: `policy-pulse-ui`) 
**Description**: A contextual, self-updating Retrieval-Augmented Generation (RAG) chatbot platform. It allows users to ask questions related to Indian government policies and get responses tailored to specific "personas" (e.g., Trade and Small Business, General Citizen). It continuously updates its knowledge base by scraping and ingesting policy-related data from official government RSS feeds and PDF reports (like PRS Legislative Research).

---

## 2. Technology Stack & Architecture

### Backend: Spring Boot application (Java 21)
- **Framework:** Spring Boot 3.3.5 (Web, Data JPA, Validation)
- **API Engine:** Spring for GraphQL (Exposing `/graphql` endpoint)
- **Message Broker integration:** Spring Kafka (using `policy-pulse-group` for decoupled document processing event topics like `session-data-scraped`)
- **Database Engine:** MySQL 8.4 via `mysql-connector-j` with Flyway database migration (`flyway-core`).
- **AI/LLM Library:** `langchain4j` and `langchain4j-ollama` (v0.35.0).
- **Web Scraping/Parsing:** `rome` (RSS parsing), `jsoup` (HTML parsing), and `pdfbox` (PDF parsing).
- **Build tool:** Maven (with `pom.xml` configured for Java 21).

### Frontend: React Single Page Application (TypeScript)
- **Build Tool:** Vite 5.4.8.
- **Framework:** React 18.3.1.
- **Testing:** Vitest and Testing Library.
- **API Client:** Minimal custom GraphQL fetch client (`frontend/src/api/graphql.ts`).

### Infrastructure: Docker Compose
Provides a self-contained runtime environment comprising:
1. **mysql** (`mysql:8.4`): Main relational database storing session data, metadata, and document pointers.
2. **kafka** (`apache/kafka:3.9.2`): Message broker using KRaft mode (no Zookeeper) configured for local/host dev.
3. **ollama** (`ollama/ollama:latest`): Local LLM inference server binding to `11434` with 6GB memory limit.

---

## 3. Deep Dive: Key Modules & Workflows

### A. Data Ingestion & Scraping (`com.policypulse.scrape.HybridGovDataCollector`)
Responsible for automatically gathering Indian government policy data. Driven by a parliamentary "session" chronology, the system evaluates three sessions a year.
- **RSS Feeds:** Reads RSS feed URLs defined in `src/main/resources/gov_dataset/rss.txt` (pointing to `services.india.gov.in` feeds across various categories). It utilizes Rome to parse standard RSS xml feeds.
- **PRS India PDFs:** Scrapes the PRS Legislative Research Monthly Policy Review page via Jsoup. It strictly filters links looking for `.pdf` URLs that belong to `prsindia.org` and explicitly include the current year and the string "monthly policy review" to prevent downloading unrelated bills or committee reports. Uses Apache PDFBox to strip text from the PDF.
- **Deduplication:** A secure fingerprint (SHA-256 hash or hashcode fallback) validates if a document is already ingested (built using the document URL, published date, title, and partial raw text).
- **Text Filtering:** Utilizes an `EnglishTextFilter` to ensure non-English or corrupted entries do not pollute the RAG model vector space.

### B. Vector Processing and RAG (`com.policypulse.rag.*`)
- **Embedding Generation:** Leverages Ollama integration using the `nomic-embed-text` embedding model to generate vectors from text chunks in `TextChunker`.
- **Vector Store:** Currently using a simplified `InMemoryVectorIndexService` loaded at runtime for nearest neighbor (`EmbeddingMatch`) queries. 
- **LLM Inference:** Integrates with `llama3:8b-instruct-q4_K_M` running on Ollama for conversational inferences (`OllamaChatModel`).
- **Chat Prompt Engineering:** Uses strict persona-aware templating. The `RagChatService` embeds the scraped context and user's requested `Persona` (e.g., `INDUSTRY_AND_LOGISTICS`) and forces the LLM to restrict responses to the scraped data contexts. It explicitly instructs the model to mention uncertainty if context is weak. 

### C. The GraphQL API Layer (`src/main/resources/graphql/schema.graphqls`)
The API defines three main queries and one mutation:
1. **Query `systemStatus`**: Retrieves high-level state (current year, active sessions, vector context alignment year).
2. **Query `sessions(year)`**: Gets metadata for chronological session cycles (SESSION_1, SESSION_2, etc).
3. **Query `ingestionDiagnostics`**: Provides runtime counters for observability detailing RSS feeds and PRS PDFs checked, accepted, and rejected.
4. **Mutation `askQuestion(input: AskQuestionInput!)`**: Consumes a `Persona` enum and a `question` string. Returns a `ChatResponse` containing the conversational `answer`, the specific knowledge base `sources` (chunk ids for transparency), and `sessionScope`.

### D. Data Entities & Chronology
- The application conceptually tracks Indian parliamentary calendars using "Session" bounds defined in `application.yml`:
  - `session1`: Feb 1
  - `session2`: Jun 1
  - `session3`: Oct 1
- **Flyway Migrations** track tables for documents, sessions, and deduplication (see scripts under `resources/db/migration`).

---

## 4. Frontend Application (`frontend/src/App.tsx`)
- Provides a clean, minimalist web interface for interacting with the backend.
- Displays dynamic charts/statuses (like "Ingestion (last run)").
- Exposes a form mapping to the `askQuestion` GraphQL mutation. Users select their domain "Persona" via a dropdown and query the LLM. 
- Results display the LLM's answer alongside the exact chunk references it utilized, providing source transparency.

## 5. Potential Extensibility & Future Work
- **Vector Store Replacement:** The current `InMemoryVectorIndexService` is meant for early phases. Replacing it with Milvus, pgvector, or ChromaDB for persistent vector persistence.
- **Kafka Consumers:** The `Kafka` broker setup implies that scraping (`session-data-scraped` topic) and embedding processes will be deeply decoupled into producer/consumer services in the future.
- **UI Expansion:** A graph dashboard showing metrics over time based on the GraphQL `ingestionDiagnostics`.

---
*Created by AI to provide immediate context on system intent, architecture, and component relationships.*

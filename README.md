# Policy Pulse: Indian Parliament Policy Querying (RAG)

Policy Pulse is a professional, self-updating Retrieval-Augmented Generation (RAG) platform. It allows users to ask natural-language questions about Indian government policies and receive accurate, persona-aware answers grounded in real legislative data.

The system maintains a **rolling 24-month window** of policy data, continuously ingesting from official RSS feeds and PRS Legislative Research reports.

---

## 1. Prerequisites (For a Fresh Machine)

Before running the project, ensure your machine has the following tools installed and verified.

### A. Java 21 SDK
- **Download**: [Adoptium (Temurin)](https://adoptium.net/temurin/releases/?version=21) or [Azul Zulu](https://www.azul.com/downloads/?version=java-21-lts&package=jdk).
- **Verification**: Run `java -version`. You should see `openjdk version "21.x.x"`.

### B. Node.js (LTS) & npm
- **Download**: [Node.js Official Site](https://nodejs.org/). Version 20.x or higher is recommended.
- **Verification**: Run `node -v` and `npm -v`.

### C. Docker & Docker Desktop
- **Download**: [Docker Desktop](https://www.docker.com/products/docker-desktop/).
- **Requirement**: Ensure the Docker daemon is running before starting the infrastructure.

---

## 2. Technology Stack

### Backend: Spring Boot (Java 21)
- **Framework**: Spring Boot 3.3.5
- **RAG Implementation**: LangChain4j (v0.35.0)
- **Database**: MySQL 8.4 (Persistence) & ChromaDB 0.4.24 (Vector Store)
- **Message Broker**: Apache Kafka 3.9.2 (Asynchronous data ingestion)
- **Parsing**: Apache PDFBox (PDFs) & Rome (RSS feeds)

### Frontend: React (TypeScript)
- **Build Tool**: Vite 5.4.8
- **Routing**: React Router 7
- **Styling**: Modern CSS with Glassmorphism and Animations
- **Testing**: Vitest & React Testing Library

---

## 3. Infrastructure Setup (Docker)

All auxiliary services are managed via Docker Compose.

1.  **Start the Services**:
    ```powershell
    docker-compose up -d
    ```
    This spins up:
    - **MySQL** (Port 3306): Stores document metadata.
    - **Kafka** (Port 9092): Handles ingestion events.
    - **ChromaDB** (Port 8000): Stores vector embeddings.
    - **Ollama** (Port 11435): Local LLM inference.
    - **Kafka UI** (Port 8081): Monitor Kafka topics.

2.  **Pull AI Models** (Required for first run):
    Ollama requires the models to be downloaded into the container volume.
    ```powershell
    docker exec -it policy-pulse-ollama ollama pull nomic-embed-text
    docker exec -it policy-pulse-ollama ollama pull llama3.1:8b
    ```

---

## 4. Running the Application

### Step 1: Start the Backend
Open a terminal in the project root. The project uses the Maven wrapper (`mvnw`), so you don't need Maven pre-installed.
```powershell
.\mvnw.cmd spring-boot:run
```
*The backend will automatically perform Flyway migrations and start the 24-month rolling ingestion cycle.*

### Step 2: Start the Frontend
Open a second terminal, navigate to the `frontend` folder, and start the development server.
```powershell
cd frontend
npm install
npm run dev
```

### Step 3: Access the UI
Open your browser to the URL provided by Vite (usually `http://localhost:5173` or `http://localhost:5174`).

- **Home Page**: Overview of the project and technical RAG pipeline.
- **Chatbot Page**: Select a persona and ask questions about policy.

---

## 5. Maintenance & Observability

- **Database**: Connect to MySQL at `127.0.0.1:3306` using `root` / `root_pass`.
- **Kafka Monitoring**: Visit `http://localhost:8081` to see real-time message flow.
- **24-Month Window**: The system automatically evicts documents older than 24 months and scrapes missing data on the 1st of every month.

---

## 6. Troubleshooting

- **CORS Errors**: Ensure the backend is running and the `application.yml` matches your frontend origin.
- **Ollama Connection**: If the LLM doesn't respond, verify port `11435` is accessible and the models are pulled.
- **ChromaDB Errors**: Ensure the `chroma_data` volume is created and persistent.

---
**GitHub**: [TheRealBrian0](https://github.com/TheRealBrian0) | **LeetCode**: [Warlocked](https://leetcode.com/u/Warlocked/)
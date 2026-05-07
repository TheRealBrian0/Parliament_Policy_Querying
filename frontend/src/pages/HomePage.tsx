import { Link } from "react-router-dom";

const PIPELINE_STEPS = [
  {
    title: "Data Extraction & Web Scraping",
    bullets: [
      "A hybrid collector scrapes policy PDFs from PRS Legislative Research (prsindia.org) and ingests items from curated RSS feeds published by Indian parliamentary bodies.",
      "The scraper enforces a rolling 24-month window: on the 1st of each month a CRON-scheduled job computes which months should be present, scrapes any missing months, and evicts months that have aged out of the window.",
      "Each scraped document is tagged with its source URL, publication date, title, and the month-ID foreign key that links it to the relational metadata layer.",
    ],
  },
  {
    title: "Document Processing & Parsing",
    bullets: [
      "Raw PDF binaries are parsed using Apache PDFBox, which extracts the full UTF-8 text content while preserving paragraph boundaries and whitespace.",
      "Metadata such as the document title, source URL, publication timestamp, and the originating month are persisted in a MySQL relational store via JPA/Hibernate entities (SessionDocumentEntity).",
      "Flyway-managed migrations guarantee the schema evolves safely across deployments, with foreign-key constraints enforcing referential integrity between months, documents, and chunks.",
    ],
  },
  {
    title: "Text Chunking",
    bullets: [
      "Extracted text is split into fixed-size overlapping chunks (configurable window size with a stride overlap) so that semantic context is not lost at chunk boundaries.",
      "Each chunk is assigned a deterministic ID derived from the document ID and chunk index, enabling idempotent re-ingestion without duplication.",
      "Chunk metadata — including the parent document ID, month-ID, and positional index — is stored in MySQL (SessionChunkEntity) for lineage tracing and targeted eviction.",
    ],
  },
  {
    title: "Embedding Generation",
    bullets: [
      "Each text chunk is transformed into a high-dimensional vector (768-d float array) using the nomic-embed-text model served locally by Ollama.",
      "Embedding requests are dispatched via Ollama's REST API (/api/embeddings endpoint) running in a dedicated Docker container with a 6 GB memory ceiling.",
      "The embedding model converts natural-language text into a geometric representation where semantically similar passages occupy nearby regions in vector space, enabling retrieval by meaning rather than exact keyword match.",
    ],
  },
  {
    title: "Vector Storage (ChromaDB)",
    bullets: [
      "Generated embeddings and their associated chunk text are upserted into a ChromaDB collection via its REST API, with each vector keyed by the chunk's deterministic ID.",
      "ChromaDB provides approximate nearest-neighbour search over the stored vectors, enabling sub-second retrieval across thousands of policy document chunks.",
      "When a month is evicted from the rolling window, all vectors belonging to that month are batch-deleted from ChromaDB via metadata filtering on the month-ID field.",
    ],
  },
  {
    title: "Query & Retrieval (RAG)",
    bullets: [
      "When a user submits a question, the query text is embedded using the same nomic-embed-text model to produce a query vector in the identical 768-d space.",
      "ChromaDB performs a cosine-similarity search against the stored chunk embeddings and returns the Top-K most relevant chunks (typically K = 5), along with their original text and metadata.",
      "This retrieval step grounds the language model in factual, up-to-date policy content — preventing hallucination and ensuring answers are evidence-backed.",
    ],
  },
  {
    title: "Answer Generation (LLM)",
    bullets: [
      "The retrieved chunks are assembled into a structured prompt that includes the user's persona (e.g., Student, Finance Professional, General Citizen), the original question, and the Top-K context passages.",
      "This prompt is sent to Llama 3.1 (8B) running locally on Ollama, which generates a persona-aware, contextualised natural-language answer tailored to the user's domain expertise level.",
      "The final response is returned via a GraphQL mutation alongside the source chunk IDs, giving the user full transparency into which documents informed the answer.",
    ],
  },
];

export default function HomePage() {
  return (
    <>
      {/* ── Hero ─────────────────────────────────────────── */}
      <section className="hero">
        <h1 className="hero__title">Policy Pulse</h1>
        <p className="hero__tagline">Bridging the Gap Between <b>Legislation</b> and <b>Conversation</b></p>
        <Link to="/chatbot">
          <button className="hero__cta" type="button">Try Now!</button>
        </Link>
      </section>

      {/* ── About ────────────────────────────────────────── */}
      <section className="about animate-fade-in-up">
        <h2 className="about__heading">What is Policy Pulse?</h2>
        <p className="about__text">
          Policy Pulse is a Retrieval-Augmented Generation (RAG) system that continuously ingests
          Indian parliamentary policy documents — bills, committee reports, budget analyses, and
          legislative summaries — from PRS Legislative Research and official RSS feeds. It maintains
          a rolling 24-month window of the most current policy data, embeds it into a vector
          database, and pairs it with a locally-hosted large language model so that anyone — from
          students to industry professionals — can ask natural-language questions and receive
          accurate, persona-aware answers grounded in real policy text.
        </p>
      </section>

      {/* ── Pipeline ─────────────────────────────────────── */}
      <section className="pipeline">
        <h2 className="pipeline__heading">How It Works</h2>
        <p className="pipeline__subheading">
          End-to-end data flow — from raw government PDFs to AI-generated answers
        </p>

        <div className="pipeline__grid stagger">
          {PIPELINE_STEPS.map((step, i) => (
            <article className="pipeline-card" key={i}>
              <div className="pipeline-card__step">{i + 1}</div>
              <h3 className="pipeline-card__title">{step.title}</h3>
              <div className="pipeline-card__body">
                <ul>
                  {step.bullets.map((b, j) => (
                    <li key={j}>{b}</li>
                  ))}
                </ul>
              </div>
            </article>
          ))}
        </div>
      </section>
    </>
  );
}

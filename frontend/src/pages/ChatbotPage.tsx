import { useCallback, useEffect, useState } from "react";
import { graphqlRequest } from "../api/graphql";
import { PERSONA_OPTIONS } from "../personas";

type SystemStatus = {
  currentYear: number;
  currentMonth: number;
  ingestedMonthCount: number;
  statusMessage: string;
};

type AskResult = {
  answer: string;
  sources: string[];
};

const STATUS_QUERY = `
  query {
    systemStatus { currentYear currentMonth ingestedMonthCount statusMessage }
  }
`;

const ASK_MUTATION = `
  mutation Ask($persona: Persona!, $question: String!) {
    askQuestion(input: { persona: $persona, question: $question }) {
      answer
      sources
    }
  }
`;

export default function ChatbotPage() {
  const [persona, setPersona] = useState(PERSONA_OPTIONS[5]!.value);
  const [question, setQuestion] = useState("");
  const [status, setStatus] = useState<SystemStatus | null>(null);
  const [result, setResult] = useState<AskResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [statusLoading, setStatusLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadStatus = useCallback(async () => {
    setStatusLoading(true);
    setError(null);
    try {
      const data = await graphqlRequest<{
        systemStatus: SystemStatus;
      }>(STATUS_QUERY);
      setStatus(data.systemStatus);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load status");
    } finally {
      setStatusLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadStatus();
  }, [loadStatus]);

  async function onAsk(e: React.FormEvent) {
    e.preventDefault();
    if (!question.trim()) return;
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const data = await graphqlRequest<{ askQuestion: AskResult }>(ASK_MUTATION, {
        persona,
        question: question.trim(),
      });
      setResult(data.askQuestion);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Request failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="chatbot-page">
      <h1>Policy Pulse Chatbot</h1>
      <p className="page-subtitle">Ask questions about current Indian policy — powered by RAG</p>

      {/* ── Status strip ─────────────────────────────────── */}
      {statusLoading && <p className="status-loading">Loading status…</p>}
      {!statusLoading && status && (
        <div className="status-strip">
          <span className="status-chip">
            <span className="status-chip__dot" />
            Year: {status.currentYear}
          </span>
          <span className="status-chip">
            <span className="status-chip__dot" />
            Month: {status.currentMonth}
          </span>
          <span className="status-chip">
            <span className="status-chip__dot" />
            Ingested: {status.ingestedMonthCount} months
          </span>
          <span className="status-chip">
            <span className="status-chip__dot" />
            {status.statusMessage}
          </span>
        </div>
      )}

      {/* ── Error banner ─────────────────────────────────── */}
      {error && (
        <div className="error-banner" role="alert">
          {error}
        </div>
      )}

      {/* ── Ask form ─────────────────────────────────────── */}
      <form className="chat-card" onSubmit={onAsk}>
        <h2>Ask a question</h2>

        <label htmlFor="persona-select">Persona</label>
        <select
          id="persona-select"
          value={persona}
          onChange={(e) => setPersona(e.target.value)}
        >
          {PERSONA_OPTIONS.map((p) => (
            <option key={p.value} value={p.value} title={p.hint}>
              {p.label}
            </option>
          ))}
        </select>
        <p className="persona-hint">
          {PERSONA_OPTIONS.find((p) => p.value === persona)?.hint}
        </p>

        <label htmlFor="question-input">Question</label>
        <textarea
          id="question-input"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="e.g. How might recent policy affect prices in my sector?"
          rows={5}
        />

        <button className="btn-ask" type="submit" disabled={loading || !question.trim()}>
          {loading ? "Thinking…" : "Ask"}
        </button>
      </form>

      {/* ── Answer ───────────────────────────────────────── */}
      {result && (
        <section className="answer-card" aria-label="Answer">
          <h2>Answer</h2>
          <div className="answer-body">{result.answer}</div>
          {result.sources.length > 0 && (
            <div className="sources">
              <h3>Sources (chunk refs)</h3>
              <ul>
                {result.sources.map((s) => (
                  <li key={s}>
                    <code>{s}</code>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>
      )}
    </div>
  );
}

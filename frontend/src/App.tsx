import { useCallback, useEffect, useState } from "react";
import { graphqlRequest } from "./api/graphql";
import { PERSONA_OPTIONS } from "./personas";

type SystemStatus = {
  currentYear: number;
  currentMonth: number;
  ingestedMonthCount: number;
  statusMessage: string;
};

type IngestionDiagnostics = {
  lastRunAt: string | null;
  rssFeedsChecked: number;
  rssAccepted: number;
  rssRejected: number;
  prsPdfLinksChecked: number;
  prsPdfAccepted: number;
  prsPdfRejected: number;
  totalPublished: number;
};

type AskResult = {
  answer: string;
  sources: string[];
};

const STATUS_QUERY = `
  query {
    systemStatus { currentYear currentMonth ingestedMonthCount statusMessage }
    ingestionDiagnostics {
      lastRunAt rssFeedsChecked rssAccepted rssRejected
      prsPdfLinksChecked prsPdfAccepted prsPdfRejected totalPublished
    }
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

const SYNC_MUTATION = `
  mutation {
    triggerIngestion
  }
`;

export default function App() {
  const [persona, setPersona] = useState(PERSONA_OPTIONS[5]!.value);
  const [question, setQuestion] = useState("");
  const [status, setStatus] = useState<SystemStatus | null>(null);
  const [diag, setDiag] = useState<IngestionDiagnostics | null>(null);
  const [result, setResult] = useState<AskResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [statusLoading, setStatusLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadStatus = useCallback(async () => {
    setStatusLoading(true);
    setError(null);
    try {
      const data = await graphqlRequest<{
        systemStatus: SystemStatus;
        ingestionDiagnostics: IngestionDiagnostics;
      }>(STATUS_QUERY);
      setStatus(data.systemStatus);
      setDiag(data.ingestionDiagnostics);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load status");
    } finally {
      setStatusLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadStatus();
  }, [loadStatus]);

  async function onSync() {
    setSyncing(true);
    setError(null);
    try {
      await graphqlRequest(SYNC_MUTATION);
      await loadStatus();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Sync failed");
    } finally {
      setSyncing(false);
    }
  }

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
    <main className="container">
      <header className="header">
        <h1>Policy Pulse RAG</h1>
        <p className="subtitle">India policy context — persona-aware Q&amp;A (GraphQL backend)</p>
      </header>

      <section className="panel" aria-label="System status">
        <div className="status-header">
          <h2>System status</h2>
          <button
            type="button"
            className="btn-sync"
            onClick={() => void onSync()}
            disabled={syncing || statusLoading}
          >
            {syncing ? "Syncing..." : "Sync Now"}
          </button>
        </div>

        {statusLoading && !syncing && <p className="muted">Loading…</p>}
        {!statusLoading && status && (
          <ul className="status-list">
            <li>
              <strong>Year:</strong> {status.currentYear}
            </li>
            <li>
              <strong>Month:</strong> {status.currentMonth}
            </li>
            <li>
              <strong>Ingested Months:</strong> {status.ingestedMonthCount}
            </li>
            <li>
              <strong>Status:</strong> {status.statusMessage}
            </li>
          </ul>
        )}
        {diag && (
          <div className="diag">
            <h3>Ingestion Diagnostics</h3>
            <p className="muted small">
              {diag.lastRunAt ? `Last run: ${new Date(diag.lastRunAt).toLocaleString()}` : "No collector run yet"}
            </p>
            <div className="stats-grid">
              <div className="stat-card">
                <span className="stat-label">RSS Feeds</span>
                <span className="stat-value">{diag.rssAccepted} / {diag.rssFeedsChecked}</span>
                <div className="stat-bar">
                   <div className="stat-progress" style={{ width: `${(diag.rssAccepted / (diag.rssFeedsChecked || 1)) * 100}%` }}></div>
                </div>
              </div>
              <div className="stat-card">
                <span className="stat-label">PRS PDFs</span>
                <span className="stat-value">{diag.prsPdfAccepted} / {diag.prsPdfLinksChecked}</span>
                <div className="stat-bar">
                   <div className="stat-progress" style={{ width: `${(diag.prsPdfAccepted / (diag.prsPdfLinksChecked || 1)) * 100}%` }}></div>
                </div>
              </div>
            </div>
            <p className="total-published">Total Published: <strong>{diag.totalPublished}</strong></p>
          </div>
        )}
      </section>

      <form className="panel" onSubmit={onAsk}>
        <h2>Ask a question</h2>
        <label>
          Persona (broad domain)
          <select value={persona} onChange={(e) => setPersona(e.target.value)}>
            {PERSONA_OPTIONS.map((p) => (
              <option key={p.value} value={p.value} title={p.hint}>
                {p.label}
              </option>
            ))}
          </select>
        </label>
        <p className="muted small hint">{PERSONA_OPTIONS.find((p) => p.value === persona)?.hint}</p>

        <label>
          Question
          <textarea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="e.g. How might recent policy affect prices in my sector?"
            rows={5}
          />
        </label>

        <button type="submit" disabled={loading || !question.trim()}>
          {loading ? "Thinking…" : "Ask"}
        </button>
      </form>

      {error && (
        <div className="error" role="alert">
          {error}
        </div>
      )}

      {result && (
        <section className="panel answer" aria-label="Answer">
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
    </main>
  );
}

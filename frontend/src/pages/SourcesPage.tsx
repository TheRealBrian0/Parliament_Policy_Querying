const RSS_FEEDS = [
  { cat: "Agriculture", url: "https://services.india.gov.in/feed/rss?cat_id=1&ln=en" },
  { cat: "Business & Self-Employment", url: "https://services.india.gov.in/feed/rss?cat_id=2&ln=en" },
  { cat: "Education & Learning", url: "https://services.india.gov.in/feed/rss?cat_id=3&ln=en" },
  { cat: "Health & Family Welfare", url: "https://services.india.gov.in/feed/rss?cat_id=5&ln=en" },
  { cat: "Housing & Shelter", url: "https://services.india.gov.in/feed/rss?cat_id=6&ln=en" },
  { cat: "Law & Justice", url: "https://services.india.gov.in/feed/rss?cat_id=7&ln=en" },
  { cat: "Transport & Infrastructure", url: "https://services.india.gov.in/feed/rss?cat_id=9&ln=en" },
  { cat: "Travel & Tourism", url: "https://services.india.gov.in/feed/rss?cat_id=10&ln=en" },
  { cat: "Utility Services", url: "https://services.india.gov.in/feed/rss?cat_id=12&ln=en" },
  { cat: "Social Welfare & Empowerment", url: "https://services.india.gov.in/feed/rss?cat_id=13&ln=en" },
  { cat: "Science & Technology", url: "https://services.india.gov.in/feed/rss?cat_id=14&ln=en" },
  { cat: "Finance", url: "https://services.india.gov.in/feed/rss?cat_id=16&ln=en" },
];

export default function SourcesPage() {
  return (
    <div className="sources-page">

      {/* ── Page header ──────────────────────────────────── */}
      <div className="sources-hero">
        <h1 className="sources-hero__title">Data Sources</h1>
        <p className="sources-hero__desc">
          Policy Pulse ingests data from two complementary official sources. On the 1st of every
          month, a scheduled job scrapes both channels, extracts raw text, and pushes each document
          through the Kafka ingestion pipeline where it is chunked, embedded, and stored in
          ChromaDB — ready to ground the AI's answers in real, up-to-date policy content.
        </p>
      </div>

      {/* ── Source 1: RSS Feeds ──────────────────────────── */}
      <section className="source-section animate-fade-in-up">
        <div className="source-section__label">Source 1</div>
        <h2 className="source-section__heading">
          {/* Radio-tower icon */}
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
               strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M5 12.55a11 11 0 0 1 14.08 0" />
            <path d="M1.42 9a16 16 0 0 1 21.16 0" />
            <path d="M8.53 16.11a6 6 0 0 1 6.95 0" />
            <circle cx="12" cy="20" r="1" fill="currentColor" />
          </svg>
          National Government Services Portal — RSS Feeds
        </h2>
        <p className="source-section__sub">
          <a href="https://services.india.gov.in" target="_blank" rel="noopener noreferrer">
            services.india.gov.in
          </a>{" "}
          publishes real-time RSS feeds across 12 government service categories. Our collector
          reads each feed using the <strong>Rome</strong> RSS parser, filters items by
          publication date within the active 24-month window, deduplicates by SHA-256 fingerprint,
          and forwards new items to Kafka.
        </p>

        <div className="rss-grid stagger">
          {RSS_FEEDS.map((f) => (
            <a
              key={f.url}
              href={f.url}
              target="_blank"
              rel="noopener noreferrer"
              className="rss-card"
            >
              <span className="rss-card__dot" aria-hidden="true" />
              <span className="rss-card__cat">{f.cat}</span>
              <span className="rss-card__url">{f.url.replace("https://", "")}</span>
            </a>
          ))}
        </div>
      </section>

      {/* ── Source 2: PRS India ──────────────────────────── */}
      <section className="source-section source-section--prs animate-fade-in-up">
        <div className="source-section__label">Source 2</div>
        <h2 className="source-section__heading">
          {/* File-text icon */}
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
               strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
            <polyline points="10 9 9 9 8 9" />
          </svg>
          PRS Legislative Research — Monthly Policy Reviews
        </h2>
        <p className="source-section__sub">
          <a href="https://prsindia.org" target="_blank" rel="noopener noreferrer">
            prsindia.org
          </a>{" "}
          publishes authoritative monthly policy review PDFs covering bills, committee reports,
          and budget analyses. Our collector uses <strong>Jsoup</strong> to scrape the PRS site,
          strictly filtering for <code>.pdf</code> links that contain the current year and the
          phrase "monthly policy review." Raw PDF text is then extracted using{" "}
          <strong>Apache PDFBox</strong> before being ingested.
        </p>

        <a
          href="https://prsindia.org/policy/monthly-policy-review"
          target="_blank"
          rel="noopener noreferrer"
          className="prs-card"
        >
          <div className="prs-card__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                 strokeLinecap="round" strokeLinejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
            </svg>
          </div>
          <div className="prs-card__body">
            <div className="prs-card__name">Monthly Policy Review Archive</div>
            <div className="prs-card__link">prsindia.org/policy/monthly-policy-review</div>
          </div>
          <div className="prs-card__arrow" aria-hidden="true">↗</div>
        </a>
      </section>

    </div>
  );
}

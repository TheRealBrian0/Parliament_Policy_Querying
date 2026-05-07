import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import App from "./App";

describe("App", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          data: {
            systemStatus: {
              currentYear: 2026,
              currentMonth: 5,
              ingestedMonthCount: 3,
              statusMessage: "3 months in window",
            },
            ingestionDiagnostics: {
              lastRunAt: "2026-04-10T12:00:00Z",
              rssFeedsChecked: 12,
              rssAccepted: 2,
              rssRejected: 40,
              prsPdfLinksChecked: 5,
              prsPdfAccepted: 3,
              prsPdfRejected: 2,
              totalPublished: 5,
            },
          },
        }),
      })
    );
  });

  it("loads system status and shows year", async () => {
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText(/2026/)).toBeInTheDocument();
    });
    expect(screen.getByRole("heading", { name: /Policy Pulse RAG/i })).toBeInTheDocument();
  });
});

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
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

  it("renders home page with Policy Pulse heading", async () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <App />
      </MemoryRouter>
    );
    expect(screen.getAllByText(/Policy Pulse/)[0]).toBeInTheDocument();
  });

  it("renders chatbot page with status", async () => {
    render(
      <MemoryRouter initialEntries={["/chatbot"]}>
        <App />
      </MemoryRouter>
    );
    await waitFor(() => {
      expect(screen.getByText(/2026/)).toBeInTheDocument();
    });
  });
});

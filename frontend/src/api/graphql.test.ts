import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { graphqlRequest, getGraphqlUrl } from "./graphql";

describe("graphqlRequest", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          data: { hello: "world" },
        }),
      })
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("POSTs JSON body and returns data", async () => {
    const data = await graphqlRequest<{ hello: string }>("query { hello }", { x: 1 });
    expect(data.hello).toBe("world");
    expect(fetch).toHaveBeenCalledWith(
      getGraphqlUrl(),
      expect.objectContaining({
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ query: "query { hello }", variables: { x: 1 } }),
      })
    );
  });

  it("throws on GraphQL errors array", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ errors: [{ message: "bad" }] }),
      })
    );
    await expect(graphqlRequest("q")).rejects.toThrow("bad");
  });
});

describe("getGraphqlUrl", () => {
  it("defaults to /graphql for Vite proxy", () => {
    expect(getGraphqlUrl()).toMatch(/graphql$/);
  });
});

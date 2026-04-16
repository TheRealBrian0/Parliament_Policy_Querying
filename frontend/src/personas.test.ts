import { describe, it, expect } from "vitest";
import { PERSONA_OPTIONS } from "./personas";

describe("personas", () => {
  it("defines six broad-domain personas aligned with GraphQL enum", () => {
    expect(PERSONA_OPTIONS).toHaveLength(6);
    const values = PERSONA_OPTIONS.map((p) => p.value);
    expect(values).toContain("GENERAL_CITIZEN");
    expect(values).toContain("TRADE_AND_SMALL_BUSINESS");
    expect(new Set(values).size).toBe(6);
  });
});

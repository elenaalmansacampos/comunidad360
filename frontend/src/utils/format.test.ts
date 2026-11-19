import { describe, expect, it } from "vitest";
import { formatMoney, formatPercent } from "./format";

describe("format helpers", () => {
  it("formats money with the Spanish locale", () => {
    expect(formatMoney("12345.5", "es")).toContain("12.345,50");
  });

  it("formats money with the Catalan locale", () => {
    expect(formatMoney("12345.5", "ca")).toContain("12.345,50");
  });

  it("formats percentages", () => {
    expect(formatPercent("12.3456", "es")).toContain("12,3456");
  });
});

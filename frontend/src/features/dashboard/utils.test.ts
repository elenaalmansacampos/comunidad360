import { describe, expect, it } from "vitest";
import { barHeight, monthLabel, reportPath } from "./utils";

describe("dashboard utils", () => {
  it("calculates chart bar heights", () => {
    expect(barHeight(50, 100)).toBe(50);
    expect(barHeight("0", 100)).toBe(0);
    expect(barHeight(1, 1000)).toBe(4);
  });

  it("localizes month labels", () => {
    expect(monthLabel(1, "es").toLowerCase()).toContain("ene");
    expect(monthLabel(1, "ca").toLowerCase()).toContain("gen");
  });

  it("builds the report endpoint", () => {
    expect(reportPath("abc")).toBe("/communities/abc/reports/summary");
  });
});

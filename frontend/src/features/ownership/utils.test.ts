import { describe, expect, it } from "vitest";
import type { Ownership } from "../../types/api";
import { activeOwnerships, sumActiveShares } from "./utils";

const base: Omit<Ownership, "id" | "userId" | "active" | "sharePercentage"> = {
  fullName: "Test",
  email: "test@example.com",
  ownershipType: "OWNER",
  validFrom: "2026-01-01",
  validTo: null,
  version: 0,
};

describe("ownership utils", () => {
  it("filters active ownerships", () => {
    const ownerships: Ownership[] = [
      { ...base, id: "1", userId: "u1", active: true, sharePercentage: "60.0000" },
      { ...base, id: "2", userId: "u2", active: false, sharePercentage: "40.0000" },
    ];

    expect(activeOwnerships(ownerships)).toHaveLength(1);
  });

  it("sums only active shares", () => {
    const ownerships: Ownership[] = [
      { ...base, id: "1", userId: "u1", active: true, sharePercentage: "60.0000" },
      { ...base, id: "2", userId: "u2", active: true, sharePercentage: "40.0000" },
      { ...base, id: "3", userId: "u3", active: false, sharePercentage: "10.0000" },
    ];

    expect(sumActiveShares(ownerships)).toBe(100);
  });
});

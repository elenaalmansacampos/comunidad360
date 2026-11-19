import { describe, expect, it } from "vitest";
import type { AssessmentCharge } from "../../types/api";
import { canManageFinance, isFinanceStaff, pendingChargeAmount } from "./utils";

const charge: AssessmentCharge = {
  id: "c1",
  assessmentId: "a1",
  unitId: "u1",
  unitCode: "1A",
  amount: "100.0000",
  coefficientSnapshot: "50.000000",
  paidAmount: "40.0000",
  status: "PARTIAL",
  version: 0,
};

describe("finance utils", () => {
  it("detects staff roles", () => {
    expect(isFinanceStaff(["OWNER"])).toBe(false);
    expect(isFinanceStaff(["TREASURER"])).toBe(true);
    expect(isFinanceStaff(["PRESIDENT"])).toBe(true);
  });

  it("detects management roles", () => {
    expect(canManageFinance(["PRESIDENT"])).toBe(false);
    expect(canManageFinance(["COMMUNITY_ADMIN"])).toBe(true);
    expect(canManageFinance(["TREASURER"])).toBe(true);
  });

  it("calculates pending charge amounts", () => {
    expect(pendingChargeAmount(charge)).toBe(60);
  });
});

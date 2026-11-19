import { describe, expect, it } from "vitest";
import type { BankTransaction } from "../../types/api";
import { absoluteAmount, isCredit, isReconciled } from "./utils";

const transaction: BankTransaction = {
  id: "t1",
  bankAccountId: "b1",
  valueDate: "2026-03-01",
  description: "Movimiento",
  amount: "-450.0000",
  currency: "EUR",
  status: "UNRECONCILED",
  version: 0,
};

describe("banking utils", () => {
  it("detects debit and credit amounts", () => {
    expect(isCredit("-10.00")).toBe(false);
    expect(isCredit("10.00")).toBe(true);
  });

  it("returns absolute amounts", () => {
    expect(absoluteAmount("-450.00")).toBe(450);
  });

  it("detects reconciled transactions", () => {
    expect(isReconciled(transaction)).toBe(false);
    expect(isReconciled({ ...transaction, status: "RECONCILED" })).toBe(true);
  });
});

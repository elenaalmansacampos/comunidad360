import type { BankTransaction } from "../../types/api";

export function isCredit(amount: number | string): boolean {
  return Number(amount) >= 0;
}

export function absoluteAmount(amount: number | string): number {
  return Math.abs(Number(amount));
}

export function isReconciled(transaction: BankTransaction): boolean {
  return transaction.status === "RECONCILED";
}

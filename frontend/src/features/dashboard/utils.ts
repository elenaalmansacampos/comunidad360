import type { Language } from "../../types/api";
import { localeFor } from "../../utils/language";

export function barHeight(value: number | string, max: number): number {
  const amount = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(amount) || amount <= 0 || max <= 0) {
    return 0;
  }
  return Math.max(4, (amount / max) * 100);
}

export function monthLabel(month: number, language: Language): string {
  return new Intl.DateTimeFormat(localeFor(language), { month: "short" })
    .format(new Date(2026, month - 1, 1));
}

export function reportPath(communityId: string): string {
  return `/communities/${communityId}/reports/summary`;
}

export function exportPath(
  communityId: string,
  kind: "monthly" | "members" | "incidents" | "providers",
): string {
  return `/communities/${communityId}/reports/${kind}.csv`;
}

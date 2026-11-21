import type { Language } from "../types/api";
import { localeFor } from "./language";

export function formatDate(value: string | Date, language: Language): string {
  const date = typeof value === "string" ? new Date(value) : value;
  return new Intl.DateTimeFormat(localeFor(language), {
    dateStyle: "medium",
  }).format(date);
}

export function formatDateTime(value: string | Date, language: Language): string {
  const date = typeof value === "string" ? new Date(value) : value;
  return new Intl.DateTimeFormat(localeFor(language), {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export function formatPercentValue(value: string | number, language: Language): string {
  const amount = typeof value === "string" ? Number(value) : value;
  return new Intl.NumberFormat(localeFor(language), {
    style: "percent",
    minimumFractionDigits: 1,
    maximumFractionDigits: 2,
  }).format(amount / 100);
}

export function formatMoney(value: string | number, language: Language, currency = "EUR"): string {
  const amount = typeof value === "string" ? Number(value) : value;
  return new Intl.NumberFormat(localeFor(language), {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function formatPercent(value: string | number, language: Language): string {
  const amount = typeof value === "string" ? Number(value) : value;
  return new Intl.NumberFormat(localeFor(language), {
    style: "percent",
    minimumFractionDigits: 2,
    maximumFractionDigits: 6,
  }).format(amount / 100);
}

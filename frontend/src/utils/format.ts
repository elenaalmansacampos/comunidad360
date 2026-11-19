import type { Language } from "../types/api";

const localeByLanguage: Record<Language, string> = {
  es: "es-ES",
  ca: "ca-ES",
};

export function formatDate(value: string | Date, language: Language): string {
  const date = typeof value === "string" ? new Date(value) : value;
  return new Intl.DateTimeFormat(localeByLanguage[language], {
    dateStyle: "medium",
  }).format(date);
}

export function formatDateTime(value: string | Date, language: Language): string {
  const date = typeof value === "string" ? new Date(value) : value;
  return new Intl.DateTimeFormat(localeByLanguage[language], {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export function formatPercentValue(value: string | number, language: Language): string {
  const amount = typeof value === "string" ? Number(value) : value;
  return new Intl.NumberFormat(localeByLanguage[language], {
    style: "percent",
    minimumFractionDigits: 1,
    maximumFractionDigits: 2,
  }).format(amount / 100);
}

export function formatMoney(value: string | number, language: Language, currency = "EUR"): string {
  const amount = typeof value === "string" ? Number(value) : value;
  return new Intl.NumberFormat(localeByLanguage[language], {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function formatPercent(value: string | number, language: Language): string {
  const amount = typeof value === "string" ? Number(value) : value;
  return new Intl.NumberFormat(localeByLanguage[language], {
    style: "percent",
    minimumFractionDigits: 2,
    maximumFractionDigits: 6,
  }).format(amount / 100);
}

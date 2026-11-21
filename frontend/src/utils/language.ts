import type { Language } from "../types/api";

export function normalizeLanguage(value: string): Language {
  if (value.startsWith("ca")) {
    return "ca";
  }
  if (value.startsWith("en")) {
    return "en";
  }
  return "es";
}

export function localeFor(language: Language): string {
  if (language === "ca") {
    return "ca-ES";
  }
  if (language === "en") {
    return "en-GB";
  }
  return "es-ES";
}

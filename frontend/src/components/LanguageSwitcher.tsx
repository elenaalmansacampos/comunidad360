import type { ChangeEvent } from "react";
import { useTranslation } from "react-i18next";
import { useAuthStore } from "../features/auth/authStore";
import type { Language } from "../types/api";

export function LanguageSwitcher() {
  const { t, i18n } = useTranslation();
  const status = useAuthStore((state) => state.status);
  const setLanguage = useAuthStore((state) => state.setLanguage);
  const current: Language = i18n.language.startsWith("ca") ? "ca" : "es";

  async function handleChange(event: ChangeEvent<HTMLSelectElement>) {
    const language = event.target.value as Language;
    if (status === "authenticated") {
      await setLanguage(language);
    } else {
      await i18n.changeLanguage(language);
    }
  }

  return (
    <label className="language-switcher">
      <span className="sr-only">{t("common.language")}</span>
      <select aria-label={t("common.language")} value={current} onChange={handleChange}>
        <option value="es">ES</option>
        <option value="ca">CA</option>
      </select>
    </label>
  );
}

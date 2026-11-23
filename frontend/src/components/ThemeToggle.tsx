import { useEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";
import { useTranslation } from "react-i18next";

type Theme = "light" | "dark";

const STORAGE_KEY = "comunidad360.theme";

function resolveTheme(): Theme {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === "light" || stored === "dark") {
    return stored;
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export function ThemeToggle() {
  const { t } = useTranslation();
  const [theme, setTheme] = useState<Theme>(() => {
    const current = document.documentElement.dataset.theme;
    return current === "dark" ? "dark" : current === "light" ? "light" : resolveTheme();
  });

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem(STORAGE_KEY, theme);
  }, [theme]);

  const next: Theme = theme === "dark" ? "light" : "dark";
  const label = next === "dark" ? t("common.themeDark") : t("common.themeLight");

  return (
    <button
      type="button"
      className="icon-btn theme-toggle"
      aria-label={label}
      title={label}
      onClick={() => setTheme(next)}
    >
      {theme === "dark" ? <Sun size={18} aria-hidden="true" /> : <Moon size={18} aria-hidden="true" />}
    </button>
  );
}

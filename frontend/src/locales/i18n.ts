import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import es from "./es/translation.json";
import ca from "./ca/translation.json";
import en from "./en/translation.json";

const STORAGE_KEY = "comunidad360.language";
const stored = localStorage.getItem(STORAGE_KEY);
const initialLanguage = stored === "ca" || stored === "en" ? stored : "es";

void i18n.use(initReactI18next).init({
  resources: {
    es: { translation: es },
    ca: { translation: ca },
    en: { translation: en },
  },
  lng: initialLanguage,
  fallbackLng: "es",
  supportedLngs: ["es", "ca", "en"],
  interpolation: {
    escapeValue: false,
  },
});

document.documentElement.lang = i18n.language;

i18n.on("languageChanged", (language) => {
  localStorage.setItem(STORAGE_KEY, language);
  document.documentElement.lang = language;
});

export default i18n;

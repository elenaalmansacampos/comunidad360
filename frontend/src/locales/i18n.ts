import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import es from "./es/translation.json";
import ca from "./ca/translation.json";

const STORAGE_KEY = "comunidad360.language";
const stored = localStorage.getItem(STORAGE_KEY);

void i18n.use(initReactI18next).init({
  resources: {
    es: { translation: es },
    ca: { translation: ca },
  },
  lng: stored === "ca" ? "ca" : "es",
  fallbackLng: "es",
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

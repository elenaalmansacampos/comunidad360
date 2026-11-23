import { Building2, FileCheck2, ShieldCheck, Wallet } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Outlet } from "react-router-dom";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { ThemeToggle } from "../components/ThemeToggle";

export function AuthLayout() {
  const { t } = useTranslation();
  return (
    <div className="auth-shell">
      <aside className="auth-aside">
        <div className="auth-brand">
          <span className="brand__mark brand__mark--light" aria-hidden="true">
            <Building2 size={20} />
          </span>
          <span>COMUNIDAD 360</span>
        </div>
        <div className="auth-aside__content">
          <p className="auth-aside__eyebrow">{t("auth.login.subtitle")}</p>
          <h2 className="auth-aside__title">{t("auth.login.title")}</h2>
          <ul className="auth-features">
            <li>
              <Wallet size={18} aria-hidden="true" />
              <span>{t("auth.login.featureFinance")}</span>
            </li>
            <li>
              <FileCheck2 size={18} aria-hidden="true" />
              <span>{t("auth.login.featureDocuments")}</span>
            </li>
            <li>
              <ShieldCheck size={18} aria-hidden="true" />
              <span>{t("auth.login.featureAccess")}</span>
            </li>
          </ul>
        </div>
        <p className="auth-aside__foot">© {new Date().getFullYear()} COMUNIDAD 360</p>
      </aside>
      <main className="auth-main">
        <header className="auth-top">
          <span className="brand">COMUNIDAD 360</span>
          <div className="auth-top__actions">
            <LanguageSwitcher />
            <ThemeToggle />
          </div>
        </header>
        <div className="auth-main__content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

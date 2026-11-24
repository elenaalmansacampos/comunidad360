import { Crown } from "lucide-react";
import { useTranslation } from "react-i18next";

export function PrimeNotice({ compact = false }: { compact?: boolean }) {
  const { t } = useTranslation();
  return (
    <section className={`prime-notice ${compact ? "prime-notice--compact" : ""}`}>
      <span className="prime-notice__icon" aria-hidden="true">
        <Crown size={20} />
      </span>
      <div className="prime-notice__content">
        <span className="prime-notice__eyebrow">{t("providers.primeBadge")}</span>
        <strong>{t("providers.primeTitle")}</strong>
        <p>{t("providers.primeMessage")}</p>
      </div>
      <span className="prime-notice__soon">{t("providers.primeSoon")}</span>
    </section>
  );
}

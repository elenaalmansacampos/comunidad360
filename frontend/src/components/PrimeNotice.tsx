import { Crown } from "lucide-react";
import { useTranslation } from "react-i18next";

export type PrimeFeature = "providers" | "banking" | "chat";

export function PrimeNotice({ feature = "providers", compact = false }: {
  feature?: PrimeFeature;
  compact?: boolean;
}) {
  const { t } = useTranslation();
  return (
    <section className={`prime-notice ${compact ? "prime-notice--compact" : ""}`}>
      <span className="prime-notice__icon" aria-hidden="true">
        <Crown size={20} />
      </span>
      <div className="prime-notice__content">
        <span className="prime-notice__eyebrow">{t("prime.badge")}</span>
        <strong>{t(`prime.${feature}Title`)}</strong>
        <p>{t(`prime.${feature}Message`)}</p>
      </div>
      <span className="prime-notice__soon">{t("prime.soon")}</span>
    </section>
  );
}

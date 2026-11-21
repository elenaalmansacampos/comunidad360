import { useTranslation } from "react-i18next";
import { formatPercent } from "../../utils/format";
import type { Language, StructureSummary } from "../../types/api";
import { normalizeLanguage } from "../../utils/language";

interface StructureSummaryCardProps {
  summary: StructureSummary;
}

export function StructureSummaryCard({ summary }: StructureSummaryCardProps) {
  const { t, i18n } = useTranslation();
  const language: Language = normalizeLanguage(i18n.language);

  return (
    <section
      className={`summary-card ${summary.valid ? "summary-card--valid" : "summary-card--invalid"}`}
      aria-live="polite"
    >
      <div>
        <p className="summary-card__label">{t("structure.coefficientSum")}</p>
        <strong className="summary-card__value">
          {formatPercent(Number(summary.coefficientSum), language)}
        </strong>
        <p className="summary-card__state">
          {summary.valid ? t("structure.valid") : t("structure.invalid")}
        </p>
      </div>

      <dl className="summary-card__details">
        <div>
          <dt>{t("structure.tolerance")}</dt>
          <dd>{formatPercent(Number(summary.tolerance), language)}</dd>
        </div>
        <div>
          <dt>{t("structure.activeUnits")}</dt>
          <dd>{summary.activeUnits}</dd>
        </div>
        <div>
          <dt>{t("structure.buildings")}</dt>
          <dd>{summary.buildings}</dd>
        </div>
        <div>
          <dt>{t("structure.commonElements")}</dt>
          <dd>{summary.commonElements}</dd>
        </div>
      </dl>
    </section>
  );
}

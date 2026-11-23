import { useEffect, useState } from "react";
import {
  AlertTriangle,
  ArrowDownCircle,
  ArrowUpCircle,
  CalendarDays,
  FileText,
  MessageCircle,
  Receipt,
  TrendingUp,
  Users,
  Vote,
  Wallet,
  Wrench,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { useAuthStore } from "../features/auth/authStore";
import { useDashboard } from "../features/communities/queries";
import { useCommunityStore } from "../features/communities/communityStore";
import { ReportSchedules } from "../features/reports/ReportSchedules";
import { barHeight, exportPath, monthLabel, reportPath } from "../features/dashboard/utils";
import { canViewProviders } from "../features/mvp/utils";
import { ApiError, downloadFile } from "../services/apiClient";
import type { Role } from "../types/api";
import { formatDateTime, formatMoney } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

function roleLabel(role: Role): string {
  return `roles.${role}`;
}

export function DashboardPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const dashboard = useDashboard(communityId);
  const [downloading, setDownloading] = useState(false);
  const [reportError, setReportError] = useState(false);
  const [exporting, setExporting] = useState<string | null>(null);
  const language = normalizeLanguage(i18n.language);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  if (dashboard.isLoading) {
    return <FullPageLoader />;
  }

  if (dashboard.isError) {
    const forbidden = dashboard.error instanceof ApiError && dashboard.error.status === 403;
    return (
      <div className="page">
        <Alert variant="error">
          {forbidden ? t("common.accessDenied") : t("common.unknownError")}
        </Alert>
        <Button variant="secondary" onClick={() => void dashboard.refetch()}>
          {t("common.retry")}
        </Button>
      </div>
    );
  }

  if (!dashboard.data) {
    return <div className="empty-state">{t("common.empty")}</div>;
  }

  const data = dashboard.data;
  const finance = data.finance;
  const maxMonthly = Math.max(
    0,
    ...data.monthlyFinance.flatMap((month) => [Number(month.income), Number(month.expense)]),
  );
  const isCommunityFinance = finance?.scope === "COMMUNITY";

  const modules = [
    { title: t("dashboard.finance"), text: t("dashboard.financePending"), to: `/communities/${communityId}/finance`, icon: <Wallet size={18} aria-hidden="true" /> },
    { title: t("dashboard.incidents"), text: t("dashboard.incidentsPending"), to: `/communities/${communityId}/incidents`, icon: <AlertTriangle size={18} aria-hidden="true" /> },
    { title: t("dashboard.documents"), text: t("dashboard.documentsPending"), to: `/communities/${communityId}/documents`, icon: <FileText size={18} aria-hidden="true" /> },
    { title: t("dashboard.meetings"), text: t("dashboard.meetingsPending"), to: `/communities/${communityId}/meetings`, icon: <CalendarDays size={18} aria-hidden="true" /> },
    { title: t("nav.chat"), text: t("dashboard.chatPending"), to: `/communities/${communityId}/chat`, icon: <MessageCircle size={18} aria-hidden="true" /> },
    ...(canViewProviders(data.roles)
      ? [{ title: t("nav.providers"), text: t("dashboard.providersPending"), to: `/communities/${communityId}/providers`, icon: <Wrench size={18} aria-hidden="true" /> }]
      : []),
    { title: t("nav.votings"), text: t("dashboard.votingsPending"), to: `/communities/${communityId}/votings`, icon: <Vote size={18} aria-hidden="true" /> },
  ];

  async function downloadReport() {
    if (!communityId) return;
    setReportError(false);
    setDownloading(true);
    try {
      const blob = await downloadFile(reportPath(communityId));
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "informe-comunidad.pdf";
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      setReportError(true);
    } finally {
      setDownloading(false);
    }
  }

  async function downloadCsv(
    kind: "monthly" | "members" | "incidents" | "providers",
    filename: string,
  ) {
    if (!communityId) return;
    setReportError(false);
    setExporting(kind);
    try {
      const blob = await downloadFile(exportPath(communityId, kind));
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = filename;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      setReportError(true);
    } finally {
      setExporting(null);
    }
  }

  return (
    <div className="page">
      <section className="dash-hero">
        <div>
          <p className="eyebrow">{data.community.name}</p>
          <h1>{t("dashboard.title")}</h1>
          <p className="page-subtitle">
            {t("dashboard.welcome", { name: user?.fullName ?? "" })}
          </p>
          <div className="chips dash-hero__roles">
            {data.roles.map((role) => (
              <span className="badge" key={role}>
                {t(roleLabel(role), { defaultValue: role })}
              </span>
            ))}
          </div>
        </div>
        {data.canViewReports ? (
          <div className="dash-hero__actions">
            <Button variant="secondary" loading={downloading} onClick={() => void downloadReport()}>
              <FileText size={16} aria-hidden="true" /> {t("dashboard.downloadReport")}
            </Button>
          </div>
        ) : null}
      </section>

      {reportError ? <Alert variant="error">{t("dashboard.reportError")}</Alert> : null}

      <div className="stat-grid">
        <article className="stat-card">
          <span className="stat-card__icon stat-card__icon--blue"><Users size={18} aria-hidden="true" /></span>
          <span className="stat-card__label">{t("dashboard.activeMembers")}</span>
          <strong className="stat-card__value">{data.activeMembers}</strong>
        </article>
        <article className="stat-card">
          <span className="stat-card__icon stat-card__icon--amber"><AlertTriangle size={18} aria-hidden="true" /></span>
          <span className="stat-card__label">{t("dashboard.openIncidents")}</span>
          <strong className="stat-card__value">{data.openIncidents}</strong>
          <Link className="stat-card__link" to={`/communities/${communityId}/incidents`}>
            {t("dashboard.viewIncidents")}
          </Link>
        </article>
        {finance ? (
          <article className="stat-card">
            <span className="stat-card__icon stat-card__icon--teal"><Wallet size={18} aria-hidden="true" /></span>
            <span className="stat-card__label">
              {isCommunityFinance ? t("dashboard.pendingCharges") : t("dashboard.myPendingCharges")}
            </span>
            <strong className="stat-card__value stat-card__value--small">
              {formatMoney(finance.pendingCharges, language, finance.currency)}
            </strong>
            <Link className="stat-card__link" to={`/communities/${communityId}/finance`}>
              {t("dashboard.viewFinance")}
            </Link>
          </article>
        ) : null}
        {finance && isCommunityFinance ? (
          <article className="stat-card">
            <span className="stat-card__icon stat-card__icon--indigo"><TrendingUp size={18} aria-hidden="true" /></span>
            <span className="stat-card__label">{t("dashboard.balance")}</span>
            <strong className="stat-card__value stat-card__value--small">
              {formatMoney(finance.balance, language, finance.currency)}
            </strong>
          </article>
        ) : null}
      </div>

      {data.canViewReports ? (
        <section className="dashboard-section">
          <div className="section-heading">
            <h2>{t("dashboard.exports")}</h2>
          </div>
          <div className="form-actions">
            <Button
              variant="secondary"
              loading={exporting === "monthly"}
              onClick={() => void downloadCsv("monthly", "evolucion-mensual.csv")}
            >
              {t("dashboard.exportMonthly")}
            </Button>
            <Button
              variant="secondary"
              loading={exporting === "members"}
              onClick={() => void downloadCsv("members", "miembros.csv")}
            >
              {t("dashboard.exportMembers")}
            </Button>
            <Button
              variant="secondary"
              loading={exporting === "incidents"}
              onClick={() => void downloadCsv("incidents", "incidencias.csv")}
            >
              {t("dashboard.exportIncidents")}
            </Button>
            <Button
              variant="secondary"
              loading={exporting === "providers"}
              onClick={() => void downloadCsv("providers", "proveedores.csv")}
            >
              {t("dashboard.exportProviders")}
            </Button>
          </div>
        </section>
      ) : null}

      {data.canViewReports ? (
        <ReportSchedules
          communityId={communityId ?? ""}
          canManage={data.roles.includes("COMMUNITY_ADMIN")}
        />
      ) : null}

      

      {finance ? (
        <section className="dashboard-section">
          <div className="section-heading">
            <h2>{isCommunityFinance ? t("dashboard.financialSummary") : t("dashboard.myFinance")}</h2>
            <span className="badge">{finance.fiscalYear}</span>
          </div>
          <div className="dashboard-metrics">
            {isCommunityFinance ? (
              <>
                <article className="metric-card metric-card--income">
                  <span><ArrowUpCircle size={14} aria-hidden="true" /> {t("dashboard.income")}</span>
                  <strong>{formatMoney(finance.income, language, finance.currency)}</strong>
                </article>
                <article className="metric-card metric-card--expense">
                  <span><ArrowDownCircle size={14} aria-hidden="true" /> {t("dashboard.expense")}</span>
                  <strong>{formatMoney(finance.expense, language, finance.currency)}</strong>
                </article>
                <article className="metric-card">
                  <span><Receipt size={14} aria-hidden="true" /> {t("dashboard.pendingInvoices")}</span>
                  <strong>{formatMoney(finance.invoicesPending, language, finance.currency)}</strong>
                </article>
              </>
            ) : (
              <article className="metric-card metric-card--expense">
                <span><Wallet size={14} aria-hidden="true" /> {t("dashboard.myPendingCharges")}</span>
                <strong>{formatMoney(finance.pendingCharges, language, finance.currency)}</strong>
              </article>
            )}
          </div>

          {isCommunityFinance && data.comparison ? (
            <div className="evolution-card">
              <div className="section-heading">
                <h2>{t("dashboard.comparison")}</h2>
                <span className="badge">
                  {monthLabel(data.comparison.currentMonth, language)}
                  {data.comparison.previousMonth > 0
                    ? ` vs ${monthLabel(data.comparison.previousMonth, language)}`
                    : ""}
                </span>
              </div>
              <div className="dashboard-metrics">
                <article className="metric-card metric-card--income">
                  <span><ArrowUpCircle size={14} aria-hidden="true" /> {t("dashboard.income")}</span>
                  <strong>{formatMoney(data.comparison.currentIncome, language, finance.currency)}</strong>
                  <small>
                    {t("dashboard.previousValue", {
                      value:
                        data.comparison.previousIncome != null
                          ? formatMoney(data.comparison.previousIncome, language, finance.currency)
                          : "—",
                    })}
                  </small>
                  {data.comparison.incomeDelta != null ? (
                    <small>
                      {t("dashboard.deltaValue", {
                        value: formatMoney(data.comparison.incomeDelta, language, finance.currency),
                      })}
                    </small>
                  ) : null}
                </article>
                <article className="metric-card metric-card--expense">
                  <span><ArrowDownCircle size={14} aria-hidden="true" /> {t("dashboard.expense")}</span>
                  <strong>{formatMoney(data.comparison.currentExpense, language, finance.currency)}</strong>
                  <small>
                    {t("dashboard.previousValue", {
                      value:
                        data.comparison.previousExpense != null
                          ? formatMoney(data.comparison.previousExpense, language, finance.currency)
                          : "—",
                    })}
                  </small>
                  {data.comparison.expenseDelta != null ? (
                    <small>
                      {t("dashboard.deltaValue", {
                        value: formatMoney(data.comparison.expenseDelta, language, finance.currency),
                      })}
                    </small>
                  ) : null}
                </article>
              </div>
            </div>
          ) : null}

          {isCommunityFinance && data.monthlyFinance.length > 0 ? (
            <div className="evolution-card" tabIndex={0} aria-label={t("dashboard.monthlyEvolution")}>
              <div className="section-heading">
                <h2>{t("dashboard.monthlyEvolution")}</h2>
                <div className="evolution-legend">
                  <span className="evolution-legend__income">{t("dashboard.income")}</span>
                  <span className="evolution-legend__expense">{t("dashboard.expense")}</span>
                </div>
              </div>
              <div className="evolution-chart">
                {data.monthlyFinance.map((month) => (
                  <div className="evolution-chart__month" key={month.month}>
                    <div className="evolution-chart__bars">
                      <span
                        className="evolution-chart__bar evolution-chart__bar--income"
                        style={{ height: `${barHeight(month.income, maxMonthly)}%` }}
                        title={formatMoney(month.income, language, finance.currency)}
                      />
                      <span
                        className="evolution-chart__bar evolution-chart__bar--expense"
                        style={{ height: `${barHeight(month.expense, maxMonthly)}%` }}
                        title={formatMoney(month.expense, language, finance.currency)}
                      />
                    </div>
                    <small>{monthLabel(month.month, language)}</small>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </section>
      ) : null}

      <section className="dashboard-grid">
        <article className="detail-card">
          <div className="section-heading">
            <h2>{t("dashboard.upcomingMeetings")}</h2>
            <Link className="text-link" to={`/communities/${communityId}/meetings`}>
              {t("dashboard.viewAll")}
            </Link>
          </div>
          {data.upcomingMeetings.length === 0 ? (
            <p>{t("dashboard.noUpcomingMeetings")}</p>
          ) : (
            <ul className="dashboard-list">
              {data.upcomingMeetings.map((meeting) => (
                <li key={meeting.id}>
                  <Link to={`/communities/${communityId}/meetings/${meeting.id}`}>
                    <strong>{meeting.title}</strong>
                  </Link>
                  <span>{formatDateTime(meeting.scheduledStart, language)}</span>
                  {meeting.location ? <small>{meeting.location}</small> : null}
                </li>
              ))}
            </ul>
          )}
        </article>

        <article className="detail-card">
          <div className="section-heading">
            <h2>{t("dashboard.recentIncidents")}</h2>
            <Link className="text-link" to={`/communities/${communityId}/incidents`}>
              {t("dashboard.viewAll")}
            </Link>
          </div>
          {data.recentIncidents.length === 0 ? (
            <p>{t("dashboard.noRecentIncidents")}</p>
          ) : (
            <ul className="dashboard-list">
              {data.recentIncidents.map((incident) => (
                <li key={incident.id}>
                  <Link to={`/communities/${communityId}/incidents/${incident.id}`}>
                    <strong>{incident.title}</strong>
                  </Link>
                  <span>{t(`incidents.priorities.${incident.priority}`)}</span>
                  <span className="badge">{t(`incidents.statuses.${incident.status}`)}</span>
                </li>
              ))}
            </ul>
          )}
        </article>
      </section>

      <section className="module-grid">
        {modules.map((module) => (
          <Link className="module-card module-card--link" to={module.to} key={module.title}>
            <span className="module-card__icon">{module.icon}</span>
            <h2>{module.title}</h2>
            <p>{module.text}</p>
          </Link>
        ))}
      </section>
    </div>
  );
}

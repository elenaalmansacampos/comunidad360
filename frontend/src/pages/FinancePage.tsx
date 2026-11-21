import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { Textarea } from "../components/Textarea";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunityStore } from "../features/communities/communityStore";
import {
  useAddBudgetItem,
  useApproveBudget,
  useAssessments,
  useBudgets,
  useCharges,
  useCloseFiscalYear,
  useCreateAssessment,
  useCreateBudget,
  useCreateFiscalYear,
  useCreateInvoice,
  useCreateLedgerEntry,
  useCreatePayment,
  useFinanceSummary,
  useFiscalYears,
  useInvoices,
  useIssueAssessment,
  useLedgerEntries,
  usePayments,
  usePostLedgerEntry,
  useReverseLedgerEntry,
} from "../features/finance/queries";
import { canManageFinance, isFinanceStaff, pendingChargeAmount } from "../features/finance/utils";
import { ApiError } from "../services/apiClient";
import type {
  AllocationMethod,
  Assessment,
  AssessmentCharge,
  AssessmentType,
  Budget,
  BudgetItemType,
  FinanceSummary,
  FiscalYear,
  Invoice,
  LedgerEntry,
  LedgerEntryType,
  Payment,
  PaymentMethod,
  Language,
} from "../types/api";
import { formatDate, formatMoney } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

type FinanceTab = "assessments" | "charges" | "payments" | "invoices" | "ledger" | "budgets";

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function FinancePage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const membership = user?.memberships.find((item) => item.id === communityId);
  const roles = membership?.roles ?? [];
  const staff = isFinanceStaff(roles);
  const owner = roles.includes("OWNER");
  const canManage = canManageFinance(roles);
  const [fiscalYearId, setFiscalYearId] = useState<string | undefined>();
  const [tab, setTab] = useState<FinanceTab>("assessments");
  const language = normalizeLanguage(i18n.language);

  const fiscalYears = useFiscalYears(communityId, staff);
  const summary = useFinanceSummary(communityId, staff ? fiscalYearId : undefined, staff ? Boolean(fiscalYearId) : owner);
  const assessments = useAssessments(communityId, fiscalYearId, staff && Boolean(fiscalYearId));
  const charges = useCharges(communityId, fiscalYearId, staff ? Boolean(fiscalYearId) : owner);
  const invoices = useInvoices(communityId, fiscalYearId, staff && Boolean(fiscalYearId));
  const payments = usePayments(communityId, staff);
  const ledger = useLedgerEntries(communityId, fiscalYearId, staff && Boolean(fiscalYearId));
  const budgets = useBudgets(communityId, fiscalYearId, staff && Boolean(fiscalYearId));

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  useEffect(() => {
    if (fiscalYears.data && fiscalYears.data.length > 0 && !fiscalYearId) {
      const open = fiscalYears.data.find((year) => year.status === "OPEN");
      setFiscalYearId(open?.id ?? fiscalYears.data[0].id);
    }
  }, [fiscalYears.data, fiscalYearId]);

  if (!staff && !owner) {
    return (
      <div className="page">
        <Alert variant="error">{t("common.accessDenied")}</Alert>
      </div>
    );
  }

  if (fiscalYears.isLoading || summary.isLoading) {
    return <FullPageLoader />;
  }

  if (fiscalYears.isError || summary.isError) {
    const forbidden = summary.error instanceof ApiError && summary.error.status === 403;
    return (
      <div className="page">
        <Alert variant="error">{forbidden ? t("common.accessDenied") : t("finance.error")}</Alert>
      </div>
    );
  }

  const selectedFiscalYear = fiscalYears.data?.find((year) => year.id === fiscalYearId);

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{t("finance.title")}</h1>
          <p className="page-subtitle">{t("finance.subtitle")}</p>
        </div>
        {staff ? (
          <Link className="btn btn--secondary" to={`/communities/${communityId}/banking`}>
            {t("nav.banking")}
          </Link>
        ) : null}
      </header>

      {staff ? (
        <FiscalYearBar
          communityId={communityId!}
          fiscalYears={fiscalYears.data ?? []}
          selected={selectedFiscalYear}
          canManage={canManage}
          onSelect={setFiscalYearId}
          onCreated={setFiscalYearId}
        />
      ) : null}

      {staff && (fiscalYears.data?.length ?? 0) === 0 ? (
        <div className="empty-state">{t("finance.noFiscalYearHint")}</div>
      ) : null}

      {summary.data ? <SummaryCards summary={summary.data} language={language} /> : null}

      {!staff ? (
        <section className="structure-section">
          <h2>{t("finance.tabs.charges")}</h2>
          <ChargesTable charges={charges.data ?? []} language={language} />
        </section>
      ) : null}

      {staff && (fiscalYears.data?.length ?? 0) > 0 ? (
        <>
          <nav className="tabs" aria-label={t("finance.title")}>
            {(["assessments", "charges", "payments", "invoices", "ledger", "budgets"] as FinanceTab[]).map((item) => (
              <button
                type="button"
                key={item}
                className={`tab ${tab === item ? "tab--active" : ""}`}
                onClick={() => setTab(item)}
              >
                {t(`finance.tabs.${item}`)}
              </button>
            ))}
          </nav>

          {tab === "assessments" ? (
            <AssessmentsSection
              communityId={communityId!}
              fiscalYearId={fiscalYearId}
              data={assessments.data ?? []}
              canManage={canManage}
              language={language}
            />
          ) : null}
          {tab === "charges" ? (
            <section className="structure-section">
              <h2>{t("finance.charges.title")}</h2>
              <ChargesTable charges={charges.data ?? []} language={language} />
            </section>
          ) : null}
          {tab === "payments" ? (
            <PaymentsSection
              communityId={communityId!}
              payments={payments.data ?? []}
              charges={charges.data ?? []}
              invoices={invoices.data ?? []}
              canManage={canManage}
              language={language}
            />
          ) : null}
          {tab === "invoices" ? (
            <InvoicesSection
              communityId={communityId!}
              fiscalYearId={fiscalYearId}
              invoices={invoices.data ?? []}
              canManage={canManage}
              language={language}
            />
          ) : null}
          {tab === "ledger" ? (
            <LedgerSection
              communityId={communityId!}
              fiscalYearId={fiscalYearId}
              entries={ledger.data ?? []}
              canManage={canManage}
              language={language}
            />
          ) : null}
          {tab === "budgets" ? (
            <BudgetsSection
              communityId={communityId!}
              fiscalYearId={fiscalYearId}
              budgets={budgets.data ?? []}
              canManage={canManage}
              language={language}
            />
          ) : null}
        </>
      ) : null}
    </div>
  );
}

function SummaryCards({ summary, language }: { summary: FinanceSummary; language: Language }) {
  const { t } = useTranslation();
  const cards = summary.scope === "OWN"
    ? [{ label: t("finance.summary.ownScope"), value: summary.pendingCharges }]
    : [
        { label: t("finance.summary.balance"), value: summary.balance },
        { label: t("finance.summary.income"), value: summary.income },
        { label: t("finance.summary.expense"), value: summary.expense },
        { label: t("finance.summary.pendingCharges"), value: summary.pendingCharges },
        { label: t("finance.summary.invoicesPending"), value: summary.invoicesPending },
      ];
  return (
    <section className="stat-grid finance-summary">
      {cards.map((card) => (
        <article className="stat-card" key={card.label}>
          <span className="stat-card__label">{card.label}</span>
          <strong className="stat-card__value stat-card__value--small">
            {formatMoney(card.value, language, summary.currency)}
          </strong>
        </article>
      ))}
    </section>
  );
}

function FiscalYearBar({
  communityId,
  fiscalYears,
  selected,
  canManage,
  onSelect,
  onCreated,
}: {
  communityId: string;
  fiscalYears: FiscalYear[];
  selected?: FiscalYear;
  canManage: boolean;
  onSelect: (id: string) => void;
  onCreated: (id: string) => void;
}) {
  const { t } = useTranslation();
  const [showForm, setShowForm] = useState(false);
  const closeMutation = useCloseFiscalYear(communityId);

  return (
    <section className="fiscal-bar">
      <div className="fiscal-bar__controls">
        <Select
          label={t("finance.fiscalYear")}
          value={selected?.id ?? ""}
          onChange={(event) => onSelect(event.target.value)}
        >
          {fiscalYears.map((year) => (
            <option value={year.id} key={year.id}>
              {year.year} · {t(`finance.fiscalYearStatus.${year.status}`, { defaultValue: year.status })}
            </option>
          ))}
        </Select>
        {canManage ? (
          <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>
            {t("finance.newFiscalYear")}
          </Button>
        ) : null}
        {canManage && selected?.status === "OPEN" ? (
          <Button
            variant="ghost"
            onClick={() => closeMutation.mutate(selected.id)}
            loading={closeMutation.isPending}
          >
            {t("finance.closeFiscalYear")}
          </Button>
        ) : null}
      </div>
      {closeMutation.isError ? <Alert variant="error">{t("finance.error")}</Alert> : null}
      {showForm ? <FiscalYearForm communityId={communityId} onCreated={onCreated} /> : null}
    </section>
  );
}

function FiscalYearForm({ communityId, onCreated }: { communityId: string; onCreated: (id: string) => void }) {
  const { t } = useTranslation();
  const mutation = useCreateFiscalYear(communityId);
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(String(currentYear));
  const [startDate, setStartDate] = useState(`${currentYear}-01-01`);
  const [endDate, setEndDate] = useState(`${currentYear}-12-31`);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const created = await mutation.mutateAsync({
        year: Number(year),
        startDate,
        endDate,
      });
      onCreated(created.id);
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("finance.newFiscalYear")}</h3>
      {mutation.isError ? <Alert variant="error">{financeError(mutation.error, t)}</Alert> : null}
      <div className="form-grid">
        <Input label={t("finance.year")} type="number" value={year} onChange={(e) => setYear(e.target.value)} required />
        <Input label={t("finance.startDate")} type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
        <Input label={t("finance.endDate")} type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} required />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("common.save")}</Button>
      </div>
    </form>
  );
}

function AssessmentsSection({
  communityId,
  fiscalYearId,
  data,
  canManage,
  language,
}: {
  communityId: string;
  fiscalYearId?: string;
  data: Assessment[];
  canManage: boolean;
  language: Language;
}) {
  const { t } = useTranslation();
  const [showForm, setShowForm] = useState(false);
  const issueMutation = useIssueAssessment(communityId);
  return (
    <section className="structure-section">
      <div className="section-heading">
        <h2>{t("finance.tabs.assessments")}</h2>
        {canManage ? (
          <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>
            {t("finance.assessment.new")}
          </Button>
        ) : null}
      </div>
      {issueMutation.isError ? <Alert variant="error">{financeError(issueMutation.error, t)}</Alert> : null}
      {showForm ? (
        <AssessmentForm communityId={communityId} fiscalYearId={fiscalYearId} onDone={() => setShowForm(false)} />
      ) : null}
      {data.length === 0 ? (
        <div className="empty-state">{t("finance.assessment.noItems")}</div>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("finance.assessment.concept")}</th>
                <th>{t("finance.assessment.type")}</th>
                <th>{t("finance.assessment.dueDate")}</th>
                <th>{t("finance.assessment.totalAmount")}</th>
                <th>{t("finance.status")}</th>
                <th>{t("finance.assessment.allocationMethod")}</th>
                {canManage ? <th>{t("structure.fields.actions")}</th> : null}
              </tr>
            </thead>
            <tbody>
              {data.map((assessment) => (
                <tr key={assessment.id}>
                  <td data-label={t("finance.assessment.concept")}>{assessment.concept}</td>
                  <td data-label={t("finance.assessment.type")}>{t(`finance.assessmentType.${assessment.type}`)}</td>
                  <td data-label={t("finance.assessment.dueDate")}>{formatDate(assessment.dueDate, language)}</td>
                  <td data-label={t("finance.assessment.totalAmount")}>{formatMoney(assessment.totalAmount, language)}</td>
                  <td data-label={t("finance.status")}>
                    <span className="badge">{t(`finance.assessmentStatus.${assessment.status}`)}</span>
                  </td>
                  <td data-label={t("finance.assessment.allocationMethod")}>{t(`finance.allocation.${assessment.allocationMethod}`)}</td>
                  {canManage ? (
                    <td data-label={t("structure.fields.actions")}>
                      {assessment.status === "DRAFT" ? (
                        <Button variant="ghost" className="btn--small" onClick={() => issueMutation.mutate(assessment.id)}>
                          {t("finance.assessment.issue")}
                        </Button>
                      ) : (
                        <span className="muted">{t("finance.assessment.chargesCount", { count: assessment.chargesCount })}</span>
                      )}
                    </td>
                  ) : null}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function AssessmentForm({ communityId, fiscalYearId, onDone }: { communityId: string; fiscalYearId?: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useCreateAssessment(communityId);
  const [concept, setConcept] = useState("");
  const [type, setType] = useState<AssessmentType>("ORDINARY");
  const [periodStart, setPeriodStart] = useState(today());
  const [periodEnd, setPeriodEnd] = useState(today());
  const [dueDate, setDueDate] = useState(today());
  const [totalAmount, setTotalAmount] = useState("");
  const [allocationMethod, setAllocationMethod] = useState<AllocationMethod>("COEFFICIENT");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!fiscalYearId) return;
    try {
      await mutation.mutateAsync({ fiscalYearId, concept, type, periodStart, periodEnd, dueDate, totalAmount, allocationMethod });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("finance.assessment.new")}</h3>
      {mutation.isError ? <Alert variant="error">{financeError(mutation.error, t)}</Alert> : null}
      <div className="form-grid">
        <Input label={t("finance.assessment.concept")} value={concept} onChange={(e) => setConcept(e.target.value)} required />
        <Select label={t("finance.assessment.type")} value={type} onChange={(e) => setType(e.target.value as AssessmentType)}>
          <option value="ORDINARY">{t("finance.assessmentType.ORDINARY")}</option>
          <option value="EXTRAORDINARY">{t("finance.assessmentType.EXTRAORDINARY")}</option>
        </Select>
        <Input label={t("finance.assessment.periodStart")} type="date" value={periodStart} onChange={(e) => setPeriodStart(e.target.value)} required />
        <Input label={t("finance.assessment.periodEnd")} type="date" value={periodEnd} onChange={(e) => setPeriodEnd(e.target.value)} required />
        <Input label={t("finance.assessment.dueDate")} type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} required />
        <Input label={t("finance.assessment.totalAmount")} type="number" min="0.01" step="0.01" value={totalAmount} onChange={(e) => setTotalAmount(e.target.value)} required />
        <Select label={t("finance.assessment.allocationMethod")} value={allocationMethod} onChange={(e) => setAllocationMethod(e.target.value as AllocationMethod)}>
          <option value="COEFFICIENT">{t("finance.allocation.COEFFICIENT")}</option>
          <option value="EQUAL">{t("finance.allocation.EQUAL")}</option>
          <option value="CUSTOM">{t("finance.allocation.CUSTOM")}</option>
        </Select>
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("common.save")}</Button>
      </div>
    </form>
  );
}

function ChargesTable({ charges, language }: { charges: AssessmentCharge[]; language: Language }) {
  const { t } = useTranslation();
  if (charges.length === 0) {
    return <div className="empty-state">{t("finance.charges.empty")}</div>;
  }
  return (
    <div className="table-wrapper">
      <table className="table">
        <thead>
          <tr>
            <th>{t("finance.charges.unit")}</th>
            <th>{t("finance.charges.amount")}</th>
            <th>{t("finance.charges.paid")}</th>
            <th>{t("finance.charges.pending")}</th>
            <th>{t("finance.charges.dueDate")}</th>
            <th>{t("finance.charges.status")}</th>
          </tr>
        </thead>
        <tbody>
          {charges.map((charge) => {
            const pending = pendingChargeAmount(charge);
            return (
              <tr key={charge.id}>
                <td data-label={t("finance.charges.unit")}>{charge.unitCode ?? charge.unitId}</td>
                <td data-label={t("finance.charges.amount")}>{formatMoney(charge.amount, language)}</td>
                <td data-label={t("finance.charges.paid")}>{formatMoney(charge.paidAmount, language)}</td>
                <td data-label={t("finance.charges.pending")}>{formatMoney(pending, language)}</td>
                <td data-label={t("finance.charges.dueDate")}>{charge.dueDate ? formatDate(charge.dueDate, language) : "—"}</td>
                <td data-label={t("finance.charges.status")}>
                  <span className="badge">{t(`finance.chargeStatus.${charge.status}`)}</span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function PaymentsSection({
  communityId,
  payments,
  charges,
  invoices,
  canManage,
  language,
}: {
  communityId: string;
  payments: Payment[];
  charges: AssessmentCharge[];
  invoices: Invoice[];
  canManage: boolean;
  language: Language;
}) {
  const { t } = useTranslation();
  const [showForm, setShowForm] = useState(false);
  return (
    <section className="structure-section">
      <div className="section-heading">
        <h2>{t("finance.tabs.payments")}</h2>
        {canManage ? <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>{t("finance.payment.new")}</Button> : null}
      </div>
      {showForm ? <PaymentForm communityId={communityId} charges={charges} invoices={invoices} onDone={() => setShowForm(false)} /> : null}
      {payments.length === 0 ? <div className="empty-state">{t("finance.payment.empty")}</div> : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("finance.payment.date")}</th>
                <th>{t("finance.payment.amount")}</th>
                <th>{t("finance.payment.method")}</th>
                <th>{t("finance.payment.target")}</th>
                <th>{t("finance.status")}</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((payment) => (
                <tr key={payment.id}>
                  <td data-label={t("finance.payment.date")}>{formatDate(payment.paymentDate, language)}</td>
                  <td data-label={t("finance.payment.amount")}>{formatMoney(payment.amount, language)}</td>
                  <td data-label={t("finance.payment.method")}>{t(`finance.paymentMethod.${payment.method}`)}</td>
                  <td data-label={t("finance.payment.target")}>{payment.assessmentChargeId ?? payment.invoiceId ?? "—"}</td>
                  <td data-label={t("finance.status")}><span className="badge">{t(`finance.paymentStatus.${payment.status}`)}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function PaymentForm({ communityId, charges, invoices, onDone }: { communityId: string; charges: AssessmentCharge[]; invoices: Invoice[]; onDone: () => void }) {
  const { t, i18n } = useTranslation();
  const language = normalizeLanguage(i18n.language);
  const mutation = useCreatePayment(communityId);
  const [targetType, setTargetType] = useState<"charge" | "invoice">("charge");
  const [targetId, setTargetId] = useState("");
  const [amount, setAmount] = useState("");
  const [paymentDate, setPaymentDate] = useState(today());
  const [method, setMethod] = useState<PaymentMethod>("BANK_TRANSFER");
  const [reference, setReference] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({
        assessmentChargeId: targetType === "charge" ? targetId : undefined,
        invoiceId: targetType === "invoice" ? targetId : undefined,
        amount,
        paymentDate,
        method,
        reference: reference || undefined,
      });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("finance.payment.new")}</h3>
      {mutation.isError ? <Alert variant="error">{financeError(mutation.error, t)}</Alert> : null}
      <div className="form-grid">
        <Select label={t("finance.payment.targetType")} value={targetType} onChange={(e) => { setTargetType(e.target.value as "charge" | "invoice"); setTargetId(""); }}>
          <option value="charge">{t("finance.payment.charge")}</option>
          <option value="invoice">{t("finance.payment.invoice")}</option>
        </Select>
        <Select label={t("finance.payment.target")} value={targetId} onChange={(e) => setTargetId(e.target.value)} required>
          <option value="">—</option>
          {targetType === "charge"
            ? charges.filter((charge) => charge.status !== "PAID" && charge.status !== "CANCELLED").map((charge) => (
                <option value={charge.id} key={charge.id}>{charge.unitCode} · {formatMoney(charge.amount, language)}</option>
              ))
            : invoices.filter((invoice) => invoice.status !== "PAID" && invoice.status !== "CANCELLED").map((invoice) => (
                <option value={invoice.id} key={invoice.id}>{invoice.providerName} · {invoice.number} · {formatMoney(invoice.total, language)}</option>
              ))}
        </Select>
        <Input label={t("finance.payment.amount")} type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required />
        <Input label={t("finance.payment.date")} type="date" value={paymentDate} onChange={(e) => setPaymentDate(e.target.value)} required />
        <Select label={t("finance.payment.method")} value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
          <option value="BANK_TRANSFER">{t("finance.paymentMethod.BANK_TRANSFER")}</option>
          <option value="CASH">{t("finance.paymentMethod.CASH")}</option>
          <option value="CARD">{t("finance.paymentMethod.CARD")}</option>
          <option value="OTHER">{t("finance.paymentMethod.OTHER")}</option>
        </Select>
        <Input label={t("finance.payment.reference")} value={reference} onChange={(e) => setReference(e.target.value)} />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("common.save")}</Button>
      </div>
    </form>
  );
}

function InvoicesSection({ communityId, fiscalYearId, invoices, canManage, language }: { communityId: string; fiscalYearId?: string; invoices: Invoice[]; canManage: boolean; language: Language }) {
  const { t } = useTranslation();
  const [showForm, setShowForm] = useState(false);
  return (
    <section className="structure-section">
      <div className="section-heading">
        <h2>{t("finance.tabs.invoices")}</h2>
        {canManage ? <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>{t("finance.invoice.new")}</Button> : null}
      </div>
      {showForm ? <InvoiceForm communityId={communityId} fiscalYearId={fiscalYearId} onDone={() => setShowForm(false)} /> : null}
      {invoices.length === 0 ? <div className="empty-state">{t("finance.invoice.empty")}</div> : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("finance.invoice.provider")}</th>
                <th>{t("finance.invoice.number")}</th>
                <th>{t("finance.invoice.issueDate")}</th>
                <th>{t("finance.invoice.total")}</th>
                <th>{t("finance.status")}</th>
              </tr>
            </thead>
            <tbody>
              {invoices.map((invoice) => (
                <tr key={invoice.id}>
                  <td data-label={t("finance.invoice.provider")}>{invoice.providerName}</td>
                  <td data-label={t("finance.invoice.number")}>{invoice.number}</td>
                  <td data-label={t("finance.invoice.issueDate")}>{formatDate(invoice.issueDate, language)}</td>
                  <td data-label={t("finance.invoice.total")}>{formatMoney(invoice.total, language)}</td>
                  <td data-label={t("finance.status")}><span className="badge">{t(`finance.invoiceStatus.${invoice.status}`)}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function InvoiceForm({ communityId, fiscalYearId, onDone }: { communityId: string; fiscalYearId?: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useCreateInvoice(communityId);
  const [providerName, setProviderName] = useState("");
  const [number, setNumber] = useState("");
  const [issueDate, setIssueDate] = useState(today());
  const [dueDate, setDueDate] = useState("");
  const [subtotal, setSubtotal] = useState("");
  const [taxAmount, setTaxAmount] = useState("0");
  const [total, setTotal] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!fiscalYearId) return;
    try {
      await mutation.mutateAsync({ fiscalYearId, providerName, number, issueDate, dueDate: dueDate || undefined, subtotal, taxAmount, total });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("finance.invoice.new")}</h3>
      {mutation.isError ? <Alert variant="error">{financeError(mutation.error, t)}</Alert> : null}
      <div className="form-grid">
        <Input label={t("finance.invoice.provider")} value={providerName} onChange={(e) => setProviderName(e.target.value)} required />
        <Input label={t("finance.invoice.number")} value={number} onChange={(e) => setNumber(e.target.value)} required />
        <Input label={t("finance.invoice.issueDate")} type="date" value={issueDate} onChange={(e) => setIssueDate(e.target.value)} required />
        <Input label={t("finance.invoice.dueDate")} type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
        <Input label={t("finance.invoice.subtotal")} type="number" min="0" step="0.01" value={subtotal} onChange={(e) => setSubtotal(e.target.value)} required />
        <Input label={t("finance.invoice.tax")} type="number" min="0" step="0.01" value={taxAmount} onChange={(e) => setTaxAmount(e.target.value)} required />
        <Input label={t("finance.invoice.total")} type="number" min="0" step="0.01" value={total} onChange={(e) => setTotal(e.target.value)} required />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("common.save")}</Button>
      </div>
    </form>
  );
}

function LedgerSection({ communityId, fiscalYearId, entries, canManage, language }: { communityId: string; fiscalYearId?: string; entries: LedgerEntry[]; canManage: boolean; language: Language }) {
  const { t } = useTranslation();
  const [showForm, setShowForm] = useState(false);
  const postMutation = usePostLedgerEntry(communityId);
  const reverseMutation = useReverseLedgerEntry(communityId);
  return (
    <section className="structure-section">
      <div className="section-heading">
        <h2>{t("finance.tabs.ledger")}</h2>
        {canManage ? <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>{t("finance.ledger.new")}</Button> : null}
      </div>
      {postMutation.isError || reverseMutation.isError ? <Alert variant="error">{t("finance.error")}</Alert> : null}
      {showForm ? <LedgerForm communityId={communityId} fiscalYearId={fiscalYearId} onDone={() => setShowForm(false)} /> : null}
      {entries.length === 0 ? <div className="empty-state">{t("finance.ledger.empty")}</div> : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("finance.ledger.number")}</th>
                <th>{t("finance.ledger.entryDate")}</th>
                <th>{t("finance.ledger.type")}</th>
                <th>{t("finance.ledger.category")}</th>
                <th>{t("finance.ledger.description")}</th>
                <th>{t("finance.ledger.amount")}</th>
                <th>{t("finance.status")}</th>
                {canManage ? <th>{t("structure.fields.actions")}</th> : null}
              </tr>
            </thead>
            <tbody>
              {entries.map((entry) => (
                <tr key={entry.id}>
                  <td data-label={t("finance.ledger.number")}>{entry.entryNumber}</td>
                  <td data-label={t("finance.ledger.entryDate")}>{formatDate(entry.entryDate, language)}</td>
                  <td data-label={t("finance.ledger.type")}>{t(`finance.ledgerType.${entry.type}`)}</td>
                  <td data-label={t("finance.ledger.category")}>{entry.category}</td>
                  <td data-label={t("finance.ledger.description")}>{entry.description}</td>
                  <td data-label={t("finance.ledger.amount")}>{formatMoney(entry.amount, language)}</td>
                  <td data-label={t("finance.status")}><span className="badge">{t(`finance.ledgerStatus.${entry.status}`)}</span></td>
                  {canManage ? (
                    <td data-label={t("structure.fields.actions")}>
                      {entry.status === "DRAFT" ? <Button variant="ghost" className="btn--small" onClick={() => postMutation.mutate(entry.id)}>{t("finance.ledger.post")}</Button> : null}
                      {entry.status === "POSTED" && !entry.reversalOfId ? <Button variant="ghost" className="btn--small" onClick={() => reverseMutation.mutate(entry.id)}>{t("finance.ledger.reverse")}</Button> : null}
                    </td>
                  ) : null}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function LedgerForm({ communityId, fiscalYearId, onDone }: { communityId: string; fiscalYearId?: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useCreateLedgerEntry(communityId);
  const [entryDate, setEntryDate] = useState(today());
  const [type, setType] = useState<LedgerEntryType>("INCOME");
  const [category, setCategory] = useState("");
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!fiscalYearId) return;
    try {
      await mutation.mutateAsync({ fiscalYearId, entryDate, type, category, description, amount });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("finance.ledger.new")}</h3>
      {mutation.isError ? <Alert variant="error">{financeError(mutation.error, t)}</Alert> : null}
      <div className="form-grid">
        <Input label={t("finance.ledger.entryDate")} type="date" value={entryDate} onChange={(e) => setEntryDate(e.target.value)} required />
        <Select label={t("finance.ledger.type")} value={type} onChange={(e) => setType(e.target.value as LedgerEntryType)}>
          <option value="INCOME">{t("finance.ledgerType.INCOME")}</option>
          <option value="EXPENSE">{t("finance.ledgerType.EXPENSE")}</option>
        </Select>
        <Input label={t("finance.ledger.category")} value={category} onChange={(e) => setCategory(e.target.value)} required />
        <Input label={t("finance.ledger.amount")} type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required />
        <Textarea label={t("finance.ledger.description")} value={description} onChange={(e) => setDescription(e.target.value)} required />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("common.save")}</Button>
      </div>
    </form>
  );
}

function BudgetsSection({ communityId, fiscalYearId, budgets, canManage, language }: { communityId: string; fiscalYearId?: string; budgets: Budget[]; canManage: boolean; language: Language }) {
  const { t } = useTranslation();
  const [showForm, setShowForm] = useState(false);
  return (
    <section className="structure-section">
      <div className="section-heading">
        <h2>{t("finance.tabs.budgets")}</h2>
        {canManage ? <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>{t("finance.budget.new")}</Button> : null}
      </div>
      {showForm ? <BudgetForm communityId={communityId} fiscalYearId={fiscalYearId} onDone={() => setShowForm(false)} /> : null}
      {budgets.length === 0 ? <div className="empty-state">{t("finance.budget.empty")}</div> : (
        <div className="list-grid">
          {budgets.map((budget) => (
            <BudgetCard key={budget.id} communityId={communityId} budget={budget} canManage={canManage} language={language} />
          ))}
        </div>
      )}
    </section>
  );
}

function BudgetForm({ communityId, fiscalYearId, onDone }: { communityId: string; fiscalYearId?: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useCreateBudget(communityId);
  const [notes, setNotes] = useState("");
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!fiscalYearId) return;
    try {
      await mutation.mutateAsync({ fiscalYearId, notes: notes || undefined });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }
  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("finance.budget.new")}</h3>
      {mutation.isError ? <Alert variant="error">{financeError(mutation.error, t)}</Alert> : null}
      <div className="form-grid">
        <Textarea label={t("finance.budget.notes")} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("common.save")}</Button>
      </div>
    </form>
  );
}

function BudgetCard({ communityId, budget, canManage, language }: { communityId: string; budget: Budget; canManage: boolean; language: Language }) {
  const { t } = useTranslation();
  const [showItemForm, setShowItemForm] = useState(false);
  const approveMutation = useApproveBudget(communityId);
  return (
    <article className="form-card">
      <div className="section-heading">
        <h3>{t("finance.budget.title")} · {t(`finance.budgetStatus.${budget.status}`)}</h3>
        {canManage && budget.status === "DRAFT" ? (
          <Button variant="ghost" className="btn--small" onClick={() => approveMutation.mutate(budget.id)}>
            {t("finance.budget.approve")}
          </Button>
        ) : null}
      </div>
      <p className="muted">
        {t("finance.summary.income")}: {formatMoney(budget.totalIncome, language)} · {t("finance.summary.expense")}: {formatMoney(budget.totalExpense, language)}
      </p>
      {budget.items.length > 0 ? (
        <ul className="budget-items">
          {budget.items.map((item) => (
            <li key={item.id}>
              <span>{item.category} · {item.description}</span>
              <strong>{formatMoney(item.plannedAmount, language)}</strong>
            </li>
          ))}
        </ul>
      ) : null}
      {canManage && budget.status === "DRAFT" ? (
        <>
          <Button variant="secondary" onClick={() => setShowItemForm((value) => !value)}>{t("finance.budget.addItem")}</Button>
          {showItemForm ? <BudgetItemForm communityId={communityId} budgetId={budget.id} onDone={() => setShowItemForm(false)} /> : null}
        </>
      ) : null}
      {approveMutation.isError ? <Alert variant="error">{t("finance.error")}</Alert> : null}
    </article>
  );
}

function BudgetItemForm({ communityId, budgetId, onDone }: { communityId: string; budgetId: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useAddBudgetItem(communityId);
  const [type, setType] = useState<BudgetItemType>("INCOME");
  const [category, setCategory] = useState("");
  const [description, setDescription] = useState("");
  const [plannedAmount, setPlannedAmount] = useState("");
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({ budgetId, type, category, description, plannedAmount });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }
  return (
    <form className="form-card" onSubmit={handleSubmit}>
      {mutation.isError ? <Alert variant="error">{financeError(mutation.error, t)}</Alert> : null}
      <div className="form-grid">
        <Select label={t("finance.budget.itemType")} value={type} onChange={(e) => setType(e.target.value as BudgetItemType)}>
          <option value="INCOME">{t("finance.budgetItemType.INCOME")}</option>
          <option value="EXPENSE">{t("finance.budgetItemType.EXPENSE")}</option>
        </Select>
        <Input label={t("finance.budget.itemCategory")} value={category} onChange={(e) => setCategory(e.target.value)} required />
        <Input label={t("finance.budget.itemDescription")} value={description} onChange={(e) => setDescription(e.target.value)} required />
        <Input label={t("finance.budget.plannedAmount")} type="number" min="0" step="0.01" value={plannedAmount} onChange={(e) => setPlannedAmount(e.target.value)} required />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("common.save")}</Button>
      </div>
    </form>
  );
}

function financeError(error: unknown, t: (key: string) => string): string {
  if (error instanceof ApiError) {
    switch (error.code) {
      case "FISCAL_YEAR_EXISTS":
        return t("finance.error");
      case "FISCAL_YEAR_CLOSED":
        return t("finance.error");
      case "ASSESSMENT_ALREADY_ISSUED":
      case "COEFFICIENT_SUM_INVALID":
      case "PAYMENT_EXCEEDS":
      case "INVOICE_DUPLICATE":
        return t("finance.error");
      default:
        return t("finance.error");
    }
  }
  return t("finance.error");
}

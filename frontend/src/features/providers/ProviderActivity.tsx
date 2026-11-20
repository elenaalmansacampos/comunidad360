import { useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Alert } from "../../components/Alert";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Textarea } from "../../components/Textarea";
import {
  useAddProviderNote,
  useCreateProviderBudget,
  useCreateProviderContract,
  useProviderBudgets,
  useProviderContracts,
  useProviderHistory,
} from "./queries";
import type { Language } from "../../types/api";
import { formatDate, formatMoney } from "../../utils/format";

interface ProviderActivityProps {
  communityId: string;
  providerId: string;
  canManage: boolean;
}

function formatDay(value: string | null | undefined, language: Language): string {
  return value ? formatDate(new Date(`${value}T00:00:00`), language) : "—";
}

export function ProviderActivity({ communityId, providerId, canManage }: ProviderActivityProps) {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;
  const contracts = useProviderContracts(communityId, providerId);
  const budgets = useProviderBudgets(communityId, providerId);
  const history = useProviderHistory(communityId, providerId);

  return (
    <>
      <article className="detail-card">
        <h2>{t("providers.contracts")}</h2>
        {contracts.isError ? <Alert variant="error">{t("providers.error")}</Alert> : null}
        {contracts.data?.length ? (
          <ul className="dashboard-list">
            {contracts.data.map((contract) => (
              <li key={contract.id}>
                <strong>{contract.title}</strong>
                <span className="badge">{t(`providers.contractStatuses.${contract.status}`)}</span>
                <small>
                  {formatDay(contract.startDate, language)} — {formatDay(contract.endDate, language)}
                  {contract.amount != null ? ` · ${formatMoney(contract.amount, language)}` : ""}
                </small>
                {contract.description ? <small>{contract.description}</small> : null}
              </li>
            ))}
          </ul>
        ) : <p>{t("providers.noContracts")}</p>}
      </article>

      <article className="detail-card">
        <h2>{t("providers.budgets")}</h2>
        {budgets.isError ? <Alert variant="error">{t("providers.error")}</Alert> : null}
        {budgets.data?.length ? (
          <ul className="dashboard-list">
            {budgets.data.map((budget) => (
              <li key={budget.id}>
                <strong>{budget.title}</strong>
                <span className="badge">{t(`providers.budgetStatuses.${budget.status}`)}</span>
                <small>
                  {formatMoney(budget.amount, language)}
                  {" · "}
                  {formatDay(budget.validUntil, language)}
                </small>
                {budget.description ? <small>{budget.description}</small> : null}
              </li>
            ))}
          </ul>
        ) : <p>{t("providers.noBudgets")}</p>}
      </article>

      <article className="detail-card">
        <h2>{t("providers.history")}</h2>
        {history.isError ? <Alert variant="error">{t("providers.error")}</Alert> : null}
        {history.data?.length ? (
          <ul className="dashboard-list">
            {history.data.map((entry) => (
              <li key={`${entry.type}-${entry.referenceId}`}>
                <span className="badge">{t(`providers.historyTypes.${entry.type}`)}</span>
                <strong>{entry.title ?? t("providers.note")}</strong>
                <small>{formatDate(entry.date, language)}</small>
                {entry.status ? <small>{t(`providers.activityStatus.${entry.status}`)}</small> : null}
                {entry.amount != null ? <small>{formatMoney(entry.amount, language)}</small> : null}
                {entry.description ? <small>{entry.description}</small> : null}
              </li>
            ))}
          </ul>
        ) : <p>{t("providers.noHistory")}</p>}
      </article>

      {canManage ? (
        <>
          <ContractForm communityId={communityId} providerId={providerId} />
          <BudgetForm communityId={communityId} providerId={providerId} />
          <NoteForm communityId={communityId} providerId={providerId} />
        </>
      ) : null}
    </>
  );
}

function ContractForm({ communityId, providerId }: { communityId: string; providerId: string }) {
  const { t } = useTranslation();
  const create = useCreateProviderContract(communityId, providerId);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [amount, setAmount] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await create.mutateAsync({
        title,
        description: description || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        amount: amount ? Number(amount) : undefined,
      });
      setTitle(""); setDescription(""); setStartDate(""); setEndDate(""); setAmount("");
    } catch { /* state */ }
  }

  return (
    <form className="form-card" onSubmit={submit}>
      <h3>{t("providers.newContract")}</h3>
      {create.isError ? <Alert variant="error">{t("providers.error")}</Alert> : null}
      <div className="form-grid">
        <Input label={t("providers.contractTitle")} value={title} onChange={(e) => setTitle(e.target.value)} required />
        <Input label={t("providers.startDate")} type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        <Input label={t("providers.endDate")} type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        <Input label={t("providers.amount")} type="number" min="0" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} />
      </div>
      <Textarea label={t("providers.description")} value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
      <div className="form-actions">
        <Button type="submit" loading={create.isPending}>{t("common.save")}</Button>
      </div>
    </form>
  );
}

function BudgetForm({ communityId, providerId }: { communityId: string; providerId: string }) {
  const { t } = useTranslation();
  const create = useCreateProviderBudget(communityId, providerId);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [validUntil, setValidUntil] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await create.mutateAsync({
        title,
        description: description || undefined,
        amount: Number(amount),
        validUntil: validUntil || undefined,
      });
      setTitle(""); setDescription(""); setAmount(""); setValidUntil("");
    } catch { /* state */ }
  }

  return (
    <form className="form-card" onSubmit={submit}>
      <h3>{t("providers.newBudget")}</h3>
      {create.isError ? <Alert variant="error">{t("providers.error")}</Alert> : null}
      <div className="form-grid">
        <Input label={t("providers.budgetTitle")} value={title} onChange={(e) => setTitle(e.target.value)} required />
        <Input label={t("providers.amount")} type="number" min="0" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required />
        <Input label={t("providers.validUntil")} type="date" value={validUntil} onChange={(e) => setValidUntil(e.target.value)} />
      </div>
      <Textarea label={t("providers.description")} value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
      <div className="form-actions">
        <Button type="submit" loading={create.isPending}>{t("common.save")}</Button>
      </div>
    </form>
  );
}

function NoteForm({ communityId, providerId }: { communityId: string; providerId: string }) {
  const { t } = useTranslation();
  const create = useAddProviderNote(communityId, providerId);
  const [body, setBody] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await create.mutateAsync({ body });
      setBody("");
    } catch { /* state */ }
  }

  return (
    <form className="form-card" onSubmit={submit}>
      <h3>{t("providers.addNote")}</h3>
      {create.isError ? <Alert variant="error">{t("providers.error")}</Alert> : null}
      <Textarea label={t("providers.note")} value={body} onChange={(e) => setBody(e.target.value)} rows={3} required />
      <div className="form-actions">
        <Button type="submit" loading={create.isPending}>{t("providers.add")}</Button>
      </div>
    </form>
  );
}

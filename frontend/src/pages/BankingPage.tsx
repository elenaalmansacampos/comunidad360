import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { PrimeNotice } from "../components/PrimeNotice";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { Textarea } from "../components/Textarea";
import { useAuthStore } from "../features/auth/authStore";
import {
  useBankAccounts,
  useBankImports,
  useBankTransactions,
  useCreateBankAccount,
  useImportCsv,
  useReconcileTransaction,
  useUnreconcile,
} from "../features/banking/queries";
import { absoluteAmount, isCredit, isReconciled } from "../features/banking/utils";
import { useCommunityStore } from "../features/communities/communityStore";
import { useLedgerEntries, usePayments } from "../features/finance/queries";
import { canManageFinance, isFinanceStaff } from "../features/finance/utils";
import { ApiError } from "../services/apiClient";
import type { BankAccount, BankImportBatch, BankTransaction, Language, LedgerEntry, Payment } from "../types/api";
import { formatDate, formatMoney } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

export function BankingPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const membership = user?.memberships.find((item) => item.id === communityId);
  const roles = membership?.roles ?? [];
  const staff = isFinanceStaff(roles);
  const canManage = canManageFinance(roles);
  const language = normalizeLanguage(i18n.language);
  const [showAccountForm, setShowAccountForm] = useState(false);
  const [reconcilingId, setReconcilingId] = useState<string | null>(null);

  const accounts = useBankAccounts(communityId, staff);
  const imports = useBankImports(communityId, staff);
  const transactions = useBankTransactions(communityId, undefined, staff);
  const payments = usePayments(communityId, staff);
  const ledger = useLedgerEntries(communityId, undefined, staff);
  const unreconcile = useUnreconcile(communityId);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  if (!staff) {
    return (
      <div className="page">
        <Alert variant="error">{t("common.accessDenied")}</Alert>
      </div>
    );
  }

  if (accounts.isLoading || imports.isLoading || transactions.isLoading) {
    return <FullPageLoader />;
  }

  if (accounts.isError || imports.isError || transactions.isError) {
    return (
      <div className="page">
        <Alert variant="error">{t("banking.error")}</Alert>
      </div>
    );
  }

  const accountList = accounts.data ?? [];
  const importList = imports.data ?? [];
  const transactionList = transactions.data ?? [];
  const reconciling = transactionList.find((transaction) => transaction.id === reconcilingId);

  return (
    <div className="page">
      <Link className="breadcrumb" to={`/communities/${communityId}/finance`}>
        {t("banking.backToFinance")}
      </Link>
      <header className="page-header">
        <div>
          <h1>{t("banking.title")}</h1>
          <p className="page-subtitle">{t("banking.subtitle")}</p>
        </div>
      </header>
      <PrimeNotice feature="banking" />

      <section className="structure-section">
        <div className="section-heading">
          <h2>{t("banking.accounts")}</h2>
          {canManage ? (
            <Button variant="secondary" onClick={() => setShowAccountForm((value) => !value)}>
              {t("banking.newAccount")}
            </Button>
          ) : null}
        </div>
        {showAccountForm ? <BankAccountForm communityId={communityId!} onDone={() => setShowAccountForm(false)} /> : null}
        {accountList.length === 0 ? (
          <div className="empty-state">{t("banking.noAccounts")}</div>
        ) : (
          <div className="list-grid">
            {accountList.map((account) => (
              <article className="list-card" key={account.id}>
                <h3>{account.name}</h3>
                <p>{account.bankName ?? "—"}</p>
                <span className="badge badge--muted">{account.ibanMasked ?? account.ibanLast4 ?? "—"}</span>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="structure-section">
        <div className="section-heading">
          <h2>{t("banking.imports")}</h2>
        </div>
        {accountList.length > 0 ? (
          <ImportForm communityId={communityId!} accounts={accountList} canManage={canManage} />
        ) : null}
        <ImportHistory imports={importList} language={language} />
      </section>

      <section className="structure-section">
        <div className="section-heading">
          <h2>{t("banking.transactions")}</h2>
        </div>
        {unreconcile.isError ? <Alert variant="error">{t("banking.error")}</Alert> : null}
        <TransactionsTable
          transactions={transactionList}
          canManage={canManage}
          language={language}
          onReconcile={setReconcilingId}
          onUnreconcile={(reconciliationId) => unreconcile.mutate(reconciliationId)}
        />
        {reconciling ? (
          <ReconcileForm
            communityId={communityId!}
            transaction={reconciling}
            payments={payments.data ?? []}
            ledgerEntries={ledger.data ?? []}
            onDone={() => setReconcilingId(null)}
          />
        ) : null}
      </section>
    </div>
  );
}

function BankAccountForm({ communityId, onDone }: { communityId: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useCreateBankAccount(communityId);
  const [name, setName] = useState("");
  const [bankName, setBankName] = useState("");
  const [iban, setIban] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({ name, bankName: bankName || undefined, iban });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("banking.newAccount")}</h3>
      {mutation.isError ? <Alert variant="error">{t("banking.accountError")}</Alert> : null}
      <div className="form-grid">
        <Input label={t("banking.accountName")} value={name} onChange={(e) => setName(e.target.value)} required />
        <Input label={t("banking.bankName")} value={bankName} onChange={(e) => setBankName(e.target.value)} />
        <Input label={t("banking.iban")} value={iban} onChange={(e) => setIban(e.target.value)} required />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("common.save")}</Button>
      </div>
    </form>
  );
}

function ImportForm({ communityId, accounts, canManage }: { communityId: string; accounts: BankAccount[]; canManage: boolean }) {
  const { t } = useTranslation();
  const mutation = useImportCsv(communityId);
  const fileRef = useRef<HTMLInputElement>(null);
  const [accountId, setAccountId] = useState(accounts[0]?.id ?? "");
  const [dateColumn, setDateColumn] = useState("Fecha");
  const [descriptionColumn, setDescriptionColumn] = useState("Descripcion");
  const [amountColumn, setAmountColumn] = useState("Importe");
  const [balanceColumn, setBalanceColumn] = useState("Saldo");
  const [externalIdColumn, setExternalIdColumn] = useState("Referencia");
  const [separator, setSeparator] = useState("semicolon");
  const [dateFormat, setDateFormat] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [file, setFile] = useState<File | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!file) {
      setError(t("banking.emptyFile"));
      return;
    }
    const formData = new FormData();
    formData.append("file", file);
    formData.append("accountId", accountId);
    formData.append("dateColumn", dateColumn);
    formData.append("descriptionColumn", descriptionColumn);
    formData.append("amountColumn", amountColumn);
    if (balanceColumn) formData.append("balanceColumn", balanceColumn);
    if (externalIdColumn) formData.append("externalIdColumn", externalIdColumn);
    formData.append("separator", separator);
    if (dateFormat) formData.append("dateFormat", dateFormat);
    try {
      await mutation.mutateAsync(formData);
      setFile(null);
      if (fileRef.current) {
        fileRef.current.value = "";
      }
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("banking.importCsv")}</h3>
      {!canManage ? <Alert variant="info">{t("common.accessDenied")}</Alert> : null}
      {error ? <Alert variant="error">{error}</Alert> : null}
      {mutation.isError ? <Alert variant="error">{t("banking.importError")}</Alert> : null}
      {mutation.data ? (
        <Alert variant="success">
          {t("banking.rows")}: {mutation.data.rowCount} · {t("banking.success")}: {mutation.data.successCount} · {t("banking.duplicates")}: {mutation.data.duplicateCount} · {t("banking.errors")}: {mutation.data.errorCount}
        </Alert>
      ) : null}
      <fieldset disabled={!canManage} className="form-grid">
        <Select label={t("banking.accounts")} value={accountId} onChange={(e) => setAccountId(e.target.value)} required>
          {accounts.map((account) => (
            <option value={account.id} key={account.id}>{account.name} · {account.ibanMasked}</option>
          ))}
        </Select>
        <div className="field">
          <label className="field__label" htmlFor="bank-csv-file">{t("banking.file")}</label>
          <input
            id="bank-csv-file"
            ref={fileRef}
            className="field__input"
            type="file"
            accept=".csv,text/csv"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            required
          />
        </div>
        <Input label={t("banking.dateColumn")} value={dateColumn} onChange={(e) => setDateColumn(e.target.value)} required />
        <Input label={t("banking.descriptionColumn")} value={descriptionColumn} onChange={(e) => setDescriptionColumn(e.target.value)} required />
        <Input label={t("banking.amountColumn")} value={amountColumn} onChange={(e) => setAmountColumn(e.target.value)} required />
        <Input label={t("banking.balanceColumn")} value={balanceColumn} onChange={(e) => setBalanceColumn(e.target.value)} />
        <Input label={t("banking.externalIdColumn")} value={externalIdColumn} onChange={(e) => setExternalIdColumn(e.target.value)} />
        <Select label={t("banking.separator")} value={separator} onChange={(e) => setSeparator(e.target.value)} required>
          <option value="semicolon">{t("banking.separatorSemicolon")}</option>
          <option value="comma">{t("banking.separatorComma")}</option>
          <option value="tab">{t("banking.separatorTab")}</option>
          <option value="pipe">{t("banking.separatorPipe")}</option>
        </Select>
        <Input label={t("banking.dateFormat")} value={dateFormat} onChange={(e) => setDateFormat(e.target.value)} />
      </fieldset>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending} disabled={!canManage}>{t("banking.importCsv")}</Button>
      </div>
    </form>
  );
}

function ImportHistory({ imports, language }: { imports: BankImportBatch[]; language: Language }) {
  const { t } = useTranslation();
  if (imports.length === 0) {
    return null;
  }
  return (
    <>
      <h3>{t("banking.importHistory")}</h3>
      <div className="table-wrapper">
        <table className="table">
          <thead>
            <tr>
              <th>{t("banking.fileName")}</th>
              <th>{t("banking.importedAt")}</th>
              <th>{t("banking.rows")}</th>
              <th>{t("banking.success")}</th>
              <th>{t("banking.duplicates")}</th>
              <th>{t("banking.errors")}</th>
              <th>{t("banking.status")}</th>
            </tr>
          </thead>
          <tbody>
            {imports.map((batch) => (
              <tr key={batch.id}>
                <td data-label={t("banking.fileName")}>{batch.fileName}</td>
                <td data-label={t("banking.importedAt")}>{formatDate(batch.importedAt, language)}</td>
                <td data-label={t("banking.rows")}>{batch.rowCount}</td>
                <td data-label={t("banking.success")}>{batch.successCount}</td>
                <td data-label={t("banking.duplicates")}>{batch.duplicateCount}</td>
                <td data-label={t("banking.errors")}>{batch.errorCount}</td>
                <td data-label={t("banking.status")}>
                  <span className="badge">{t(`banking.importStatus.${batch.status}`)}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

function TransactionsTable({
  transactions,
  canManage,
  language,
  onReconcile,
  onUnreconcile,
}: {
  transactions: BankTransaction[];
  canManage: boolean;
  language: Language;
  onReconcile: (id: string) => void;
  onUnreconcile: (id: string) => void;
}) {
  const { t } = useTranslation();
  if (transactions.length === 0) {
    return <div className="empty-state">{t("banking.noTransactions")}</div>;
  }
  return (
    <div className="table-wrapper">
      <table className="table">
        <thead>
          <tr>
            <th>{t("banking.date")}</th>
            <th>{t("banking.description")}</th>
            <th>{t("banking.amount")}</th>
            <th>{t("banking.status")}</th>
            {canManage ? <th>{t("structure.fields.actions")}</th> : null}
          </tr>
        </thead>
        <tbody>
          {transactions.map((transaction) => (
            <tr key={transaction.id}>
              <td data-label={t("banking.date")}>{formatDate(transaction.valueDate, language)}</td>
              <td data-label={t("banking.description")}>{transaction.description}</td>
              <td data-label={t("banking.amount")}>
                <span className={isCredit(transaction.amount) ? "amount-positive" : "amount-negative"}>
                  {formatMoney(transaction.amount, language, transaction.currency)}
                </span>
              </td>
              <td data-label={t("banking.status")}>
                <span className={`badge ${isReconciled(transaction) ? "" : "badge--muted"}`}>
                  {t(`banking.transactionStatus.${transaction.status}`)}
                </span>
              </td>
              {canManage ? (
                <td data-label={t("structure.fields.actions")}>
                  {transaction.status === "UNRECONCILED" ? (
                    <Button variant="ghost" className="btn--small" onClick={() => onReconcile(transaction.id)}>
                      {t("banking.reconcile")}
                    </Button>
                  ) : null}
                  {transaction.status === "RECONCILED" && transaction.reconciliationId ? (
                    <Button
                      variant="ghost"
                      className="btn--small"
                      onClick={() => onUnreconcile(transaction.reconciliationId!)}
                    >
                      {t("banking.undo")}
                    </Button>
                  ) : null}
                </td>
              ) : null}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ReconcileForm({
  communityId,
  transaction,
  payments,
  ledgerEntries,
  onDone,
}: {
  communityId: string;
  transaction: BankTransaction;
  payments: Payment[];
  ledgerEntries: LedgerEntry[];
  onDone: () => void;
}) {
  const { t, i18n } = useTranslation();
  const language = normalizeLanguage(i18n.language);
  const mutation = useReconcileTransaction(communityId);
  const [targetType, setTargetType] = useState<"payment" | "ledger">("payment");
  const [targetId, setTargetId] = useState("");
  const [matchedAmount, setMatchedAmount] = useState(absoluteAmount(transaction.amount).toFixed(2));
  const [notes, setNotes] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({
        transactionId: transaction.id,
        paymentId: targetType === "payment" ? targetId : undefined,
        ledgerEntryId: targetType === "ledger" ? targetId : undefined,
        matchedAmount,
        notes: notes || undefined,
      });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  const message = mutation.error instanceof ApiError && mutation.error.code === "BANK_RECONCILIATION_AMOUNT_MISMATCH"
    ? t("banking.amountMismatch")
    : mutation.error instanceof ApiError && mutation.error.code === "BANK_ALREADY_RECONCILED"
      ? t("banking.alreadyReconciled")
      : t("banking.error");

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("banking.reconcileTitle")} · {transaction.description}</h3>
      {mutation.isError ? <Alert variant="error">{message}</Alert> : null}
      <div className="form-grid">
        <Select label={t("banking.targetType")} value={targetType} onChange={(e) => { setTargetType(e.target.value as "payment" | "ledger"); setTargetId(""); }}>
          <option value="payment">{t("banking.payment")}</option>
          <option value="ledger">{t("banking.ledger")}</option>
        </Select>
        <Select label={t("banking.target")} value={targetId} onChange={(e) => setTargetId(e.target.value)} required>
          <option value="">—</option>
          {targetType === "payment"
            ? payments.filter((payment) => payment.status === "REGISTERED").map((payment) => (
                <option value={payment.id} key={payment.id}>
                  {formatDate(payment.paymentDate, language)} · {formatMoney(payment.amount, language)}
                </option>
              ))
            : ledgerEntries.filter((entry) => entry.status === "POSTED").map((entry) => (
                <option value={entry.id} key={entry.id}>
                  #{entry.entryNumber} · {entry.category} · {formatMoney(entry.amount, language)}
                </option>
              ))}
        </Select>
        <Input label={t("banking.matchedAmount")} type="number" step="0.01" value={matchedAmount} onChange={(e) => setMatchedAmount(e.target.value)} required />
        <Textarea label={t("banking.notes")} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("banking.reconcile")}</Button>
      </div>
    </form>
  );
}

import { useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { useAuthStore } from "../features/auth/authStore";
import { canManageProviders } from "../features/mvp/utils";
import {
  useAddProviderContact,
  useProvider,
  useProviderContacts,
  useUpdateProvider,
} from "../features/providers/queries";
import { ProviderActivity } from "../features/providers/ProviderActivity";
import type { ProviderStatus } from "../types/api";

export function ProviderDetailPage() {
  const { t } = useTranslation();
  const { communityId, providerId } = useParams<{ communityId: string; providerId: string }>();
  const user = useAuthStore((state) => state.user);
  const canManage = canManageProviders(user?.memberships.find((item) => item.id === communityId)?.roles ?? []);
  const provider = useProvider(communityId, providerId);
  const contacts = useProviderContacts(communityId, providerId);
  const addContact = useAddProviderContact(communityId, providerId);
  const updateProvider = useUpdateProvider(communityId, providerId);
  const [name, setName] = useState("");
  const [role, setRole] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [primary, setPrimary] = useState(false);

  if (provider.isLoading) return <FullPageLoader />;
  if (provider.isError || !provider.data) {
    return <div className="page"><Alert variant="error">{t("providers.error")}</Alert></div>;
  }
  const data = provider.data;

  async function submitContact(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await addContact.mutateAsync({ name, role: role || undefined, email: email || undefined,
        phone: phone || undefined, primaryContact: primary });
      setName(""); setRole(""); setEmail(""); setPhone(""); setPrimary(false);
    } catch { /* state */ }
  }

  async function toggleStatus() {
    const status: ProviderStatus = data.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try { await updateProvider.mutateAsync({ status, version: data.version }); } catch { /* state */ }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <Link className="breadcrumb" to={`/communities/${communityId}/providers`}>{t("providers.backToList")}</Link>
          <h1>{data.name}</h1>
          <div className="chips">
            <span className="badge">{t(`providers.statuses.${data.status}`)}</span>
            {data.category ? <span className="badge">{data.category}</span> : null}
          </div>
        </div>
        {canManage ? (
          <Button variant="secondary" loading={updateProvider.isPending} onClick={() => void toggleStatus()}>
            {data.status === "ACTIVE" ? t("providers.deactivate") : t("providers.activate")}
          </Button>
        ) : null}
      </header>
      {updateProvider.isError ? <Alert variant="error">{t("providers.error")}</Alert> : null}
      <section className="detail-grid">
        <article className="detail-card">
          <h2>{t("providers.details")}</h2>
          <dl className="detail-list">
            <dt>{t("providers.taxId")}</dt><dd>{data.taxId ?? "—"}</dd>
            <dt>{t("common.email")}</dt><dd>{data.email ?? "—"}</dd>
            <dt>{t("providers.phone")}</dt><dd>{data.phone ?? "—"}</dd>
            <dt>{t("providers.address")}</dt><dd>{data.address ?? "—"}</dd>
          </dl>
          {data.notes ? <p className="detail-description">{data.notes}</p> : null}
        </article>
        <article className="detail-card">
          <h2>{t("providers.contacts")}</h2>
          {contacts.data?.length ? (
            <ul className="dashboard-list">
              {contacts.data.map((contact) => (
                <li key={contact.id}>
                  <strong>{contact.name}</strong>
                  <span>{contact.role ?? "—"}</span>
                  <small>{contact.email ?? contact.phone ?? ""}</small>
                  {contact.primaryContact ? <span className="badge">{t("providers.primary")}</span> : null}
                </li>
              ))}
            </ul>
          ) : <p>{t("providers.noContacts")}</p>}
        </article>
      </section>
      {canManage ? (
        <form className="form-card" onSubmit={submitContact}>
          <h3>{t("providers.addContact")}</h3>
          {addContact.isError ? <Alert variant="error">{t("providers.error")}</Alert> : null}
          <div className="form-grid">
            <Input label={t("providers.contactName")} value={name} onChange={(e) => setName(e.target.value)} required />
            <Input label={t("providers.contactRole")} value={role} onChange={(e) => setRole(e.target.value)} />
            <Input label={t("common.email")} type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            <Input label={t("providers.phone")} value={phone} onChange={(e) => setPhone(e.target.value)} />
            <label className="inline-check">
              <input type="checkbox" checked={primary} onChange={(e) => setPrimary(e.target.checked)} />
              <span>{t("providers.primary")}</span>
            </label>
          </div>
          <div className="form-actions"><Button type="submit" loading={addContact.isPending}>{t("providers.add")}</Button></div>
        </form>
      ) : null}
      <ProviderActivity communityId={communityId ?? ""} providerId={providerId ?? ""} canManage={canManage} />
    </div>
  );
}

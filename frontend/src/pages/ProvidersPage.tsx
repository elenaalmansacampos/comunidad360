import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunityStore } from "../features/communities/communityStore";
import { canManageProviders } from "../features/mvp/utils";
import { useCreateProvider, useProviders } from "../features/providers/queries";

export function ProvidersPage() {
  const { t } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const canManage = canManageProviders(user?.memberships.find((item) => item.id === communityId)?.roles ?? []);
  const [showForm, setShowForm] = useState(false);
  const providers = useProviders(communityId);

  useEffect(() => {
    if (communityId) setSelectedCommunityId(communityId);
  }, [communityId, setSelectedCommunityId]);

  if (providers.isLoading) return <FullPageLoader />;
  if (providers.isError) return <div className="page"><Alert variant="error">{t("providers.error")}</Alert></div>;

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{t("providers.title")}</h1>
          <p className="page-subtitle">{t("providers.subtitle")}</p>
        </div>
        {canManage ? (
          <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>
            {t("providers.newProvider")}
          </Button>
        ) : null}
      </header>
      {showForm && communityId ? <ProviderForm communityId={communityId} onDone={() => setShowForm(false)} /> : null}
      {providers.data?.length === 0 ? (
        <div className="empty-state">{t("providers.noProviders")}</div>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead><tr>
              <th>{t("providers.name")}</th><th>{t("providers.category")}</th>
              <th>{t("providers.taxId")}</th><th>{t("providers.status")}</th>
            </tr></thead>
            <tbody>
              {providers.data?.map((provider) => (
                <tr key={provider.id}>
                  <td data-label={t("providers.name")}>
                    <Link to={`/communities/${communityId}/providers/${provider.id}`}>{provider.name}</Link>
                  </td>
                  <td data-label={t("providers.category")}>{provider.category ?? "—"}</td>
                  <td data-label={t("providers.taxId")}>{provider.taxId ?? "—"}</td>
                  <td data-label={t("providers.status")}>
                    <span className="badge">{t(`providers.statuses.${provider.status}`)}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function ProviderForm({ communityId, onDone }: { communityId: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useCreateProvider(communityId);
  const [name, setName] = useState("");
  const [taxId, setTaxId] = useState("");
  const [category, setCategory] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({ name, taxId: taxId || undefined, category: category || undefined,
        email: email || undefined, phone: phone || undefined });
      onDone();
    } catch { /* state */ }
  }

  return (
    <form className="form-card" onSubmit={submit}>
      <h3>{t("providers.newProvider")}</h3>
      {mutation.isError ? <Alert variant="error">{t("providers.error")}</Alert> : null}
      <div className="form-grid">
        <Input label={t("providers.name")} value={name} onChange={(e) => setName(e.target.value)} required />
        <Input label={t("providers.taxId")} value={taxId} onChange={(e) => setTaxId(e.target.value)} />
        <Input label={t("providers.category")} value={category} onChange={(e) => setCategory(e.target.value)} />
        <Input label={t("common.email")} type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <Input label={t("providers.phone")} value={phone} onChange={(e) => setPhone(e.target.value)} />
      </div>
      <div className="form-actions"><Button type="submit" loading={mutation.isPending}>{t("common.save")}</Button></div>
    </form>
  );
}

import { useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { useCreateOrganization, useMyOrganizations } from "../features/organizations/queries";

export function OrganizationsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const organizations = useMyOrganizations();
  const create = useCreateOrganization();
  const [name, setName] = useState("");
  const [taxId, setTaxId] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await create.mutateAsync({ name, taxId: taxId || undefined });
      setName("");
      setTaxId("");
    } catch {
      // estado de la mutación
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{t("organizations.title")}</h1>
          <p className="page-subtitle">{t("organizations.subtitle")}</p>
        </div>
        <Button variant="secondary" onClick={() => navigate("/communities")}>
          {t("organizations.backToCommunities")}
        </Button>
      </header>

      {organizations.isLoading ? <FullPageLoader /> : null}
      {organizations.isError ? <Alert variant="error">{t("organizations.error")}</Alert> : null}

      {organizations.data?.length === 0 ? (
        <div className="empty-state">{t("organizations.empty")}</div>
      ) : null}

      {organizations.data?.length ? (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("organizations.name")}</th>
                <th>{t("organizations.taxId")}</th>
                <th>{t("organizations.communityCount")}</th>
                <th>{t("organizations.status")}</th>
                <th>{t("organizations.role")}</th>
              </tr>
            </thead>
            <tbody>
              {organizations.data.map((organization) => (
                <tr key={organization.id}>
                  <td data-label={t("organizations.name")}>
                    <Link to={`/organizations/${organization.id}`}>{organization.name}</Link>
                  </td>
                  <td data-label={t("organizations.taxId")}>{organization.taxId ?? "—"}</td>
                  <td data-label={t("organizations.communityCount")}>{organization.communityCount}</td>
                  <td data-label={t("organizations.status")}>
                    <span className="badge">{t(`organizations.statuses.${organization.status}`)}</span>
                  </td>
                  <td data-label={t("organizations.role")}>
                    {t(`organizations.roles.${organization.role}`)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      <form className="form-card" onSubmit={submit}>
        <h3>{t("organizations.new")}</h3>
        {create.isError ? <Alert variant="error">{t("organizations.error")}</Alert> : null}
        <div className="form-grid">
          <Input
            label={t("organizations.name")}
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
          <Input
            label={t("organizations.taxId")}
            value={taxId}
            onChange={(event) => setTaxId(event.target.value)}
          />
        </div>
        <div className="form-actions">
          <Button type="submit" loading={create.isPending}>{t("common.save")}</Button>
        </div>
      </form>
    </div>
  );
}

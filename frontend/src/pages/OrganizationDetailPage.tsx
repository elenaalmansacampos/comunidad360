import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunities } from "../features/communities/queries";
import {
  useAddOrganizationMember,
  useAttachCommunity,
  useDetachCommunity,
  useOrganization,
  useOrganizationCommunities,
  useOrganizationMembers,
  useRemoveOrganizationMember,
} from "../features/organizations/queries";
import type { OrganizationMemberRole } from "../types/api";

export function OrganizationDetailPage() {
  const { t } = useTranslation();
  const { organizationId } = useParams<{ organizationId: string }>();
  const user = useAuthStore((state) => state.user);
  const organization = useOrganization(organizationId);
  const communities = useOrganizationCommunities(organizationId);
  const members = useOrganizationMembers(organizationId);
  const allCommunities = useCommunities();
  const attach = useAttachCommunity(organizationId);
  const detach = useDetachCommunity(organizationId);
  const addMember = useAddOrganizationMember(organizationId);
  const removeMember = useRemoveOrganizationMember(organizationId);
  const [selectedCommunity, setSelectedCommunity] = useState("");
  const [memberEmail, setMemberEmail] = useState("");
  const [memberRole, setMemberRole] = useState<OrganizationMemberRole>("ORG_ADMIN");

  const linkedIds = new Set(communities.data?.map((community) => community.id) ?? []);
  const available = (allCommunities.data ?? []).filter(
    (community) =>
      !linkedIds.has(community.id) &&
      (community.roles.includes("COMMUNITY_ADMIN") || community.roles.includes("SUPER_ADMIN")),
  );

  useEffect(() => {
    if (!selectedCommunity && available.length > 0) {
      setSelectedCommunity(available[0].id);
    }
  }, [available, selectedCommunity]);

  if (organization.isLoading) {
    return <FullPageLoader />;
  }
  if (organization.isError || !organization.data) {
    return (
      <div className="page">
        <Alert variant="error">{t("organizations.error")}</Alert>
        <Link className="breadcrumb" to="/organizations">{t("organizations.backToOrganizations")}</Link>
      </div>
    );
  }

  const data = organization.data;
  const isAdmin = data.role === "ORG_ADMIN";

  async function submitCommunity(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedCommunity) return;
    try {
      await attach.mutateAsync(selectedCommunity);
      setSelectedCommunity("");
    } catch {
      // estado de la mutación
    }
  }

  async function submitMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await addMember.mutateAsync({ email: memberEmail, role: memberRole });
      setMemberEmail("");
    } catch {
      // estado de la mutación
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <Link className="breadcrumb" to="/organizations">{t("organizations.backToOrganizations")}</Link>
          <h1>{data.name}</h1>
          <div className="chips">
            <span className="badge">{t(`organizations.statuses.${data.status}`)}</span>
            <span className="badge">{t(`organizations.roles.${data.role}`)}</span>
            {data.taxId ? <span className="badge">{data.taxId}</span> : null}
          </div>
        </div>
      </header>

      <section className="detail-grid">
        <article className="detail-card">
          <h2>{t("organizations.communities")}</h2>
          {attach.isError ? <Alert variant="error">{t("organizations.error")}</Alert> : null}
          {communities.data?.length ? (
            <ul className="dashboard-list">
              {communities.data.map((community) => (
                <li key={community.id}>
                  <Link to={`/communities/${community.id}/dashboard`}>
                    <strong>{community.name}</strong>
                  </Link>
                  <span className="badge">{community.status}</span>
                  {isAdmin ? (
                    <Button
                      variant="secondary"
                      loading={detach.isPending}
                      onClick={() => void detach.mutateAsync(community.id)}
                    >
                      {t("organizations.detach")}
                    </Button>
                  ) : null}
                </li>
              ))}
            </ul>
          ) : <p>{t("organizations.noCommunities")}</p>}
        </article>

        <article className="detail-card">
          <h2>{t("organizations.members")}</h2>
          {addMember.isError ? <Alert variant="error">{t("organizations.error")}</Alert> : null}
          {members.data?.length ? (
            <ul className="dashboard-list">
              {members.data.map((member) => (
                <li key={member.userId}>
                  <strong>{member.fullName}</strong>
                  <span>{member.email}</span>
                  <span className="badge">{t(`organizations.roles.${member.role}`)}</span>
                  <span className="badge">{t(`organizations.memberStatuses.${member.status}`)}</span>
                  {isAdmin && member.userId !== user?.id ? (
                    <Button
                      variant="secondary"
                      loading={removeMember.isPending}
                      onClick={() => void removeMember.mutateAsync(member.userId)}
                    >
                      {t("organizations.remove")}
                    </Button>
                  ) : null}
                </li>
              ))}
            </ul>
          ) : <p>{t("organizations.noMembers")}</p>}
        </article>
      </section>

      {isAdmin ? (
        <>
          <form className="form-card" onSubmit={submitCommunity}>
            <h3>{t("organizations.attachCommunity")}</h3>
            <div className="form-grid">
              <Select
                label={t("organizations.selectCommunity")}
                value={selectedCommunity}
                onChange={(event) => setSelectedCommunity(event.target.value)}
              >
                <option value="">{t("organizations.selectCommunity")}</option>
                {available.map((community) => (
                  <option key={community.id} value={community.id}>{community.name}</option>
                ))}
              </Select>
            </div>
            <div className="form-actions">
              <Button type="submit" loading={attach.isPending} disabled={!selectedCommunity}>
                {t("organizations.attach")}
              </Button>
            </div>
          </form>

          <form className="form-card" onSubmit={submitMember}>
            <h3>{t("organizations.addMember")}</h3>
            <div className="form-grid">
              <Input
                label={t("organizations.memberEmail")}
                type="email"
                value={memberEmail}
                onChange={(event) => setMemberEmail(event.target.value)}
                required
              />
              <Select
                label={t("organizations.memberRole")}
                value={memberRole}
                onChange={(event) => setMemberRole(event.target.value as OrganizationMemberRole)}
              >
                <option value="ORG_ADMIN">{t("organizations.roles.ORG_ADMIN")}</option>
                <option value="ORG_STAFF">{t("organizations.roles.ORG_STAFF")}</option>
              </Select>
            </div>
            <div className="form-actions">
              <Button type="submit" loading={addMember.isPending}>{t("organizations.addMember")}</Button>
            </div>
          </form>
        </>
      ) : null}
    </div>
  );
}

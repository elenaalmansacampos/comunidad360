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
import { useMembers } from "../features/communities/queries";
import { useCommunityStore } from "../features/communities/communityStore";
import {
  useCreateOwnership,
  useCreateTenancy,
  useEndOwnership,
  useOwnerships,
  useRevokeTenancy,
  useTenancies,
} from "../features/ownership/queries";
import { activeOwnerships, sumActiveShares } from "../features/ownership/utils";
import { useUnit } from "../features/structure/queries";
import { ApiError } from "../services/apiClient";
import type { Language, Member, OwnershipType } from "../types/api";
import { formatDate, formatPercent } from "../utils/format";

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function UnitDetailPage() {
  const { t, i18n } = useTranslation();
  const { communityId, unitId } = useParams<{ communityId: string; unitId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const membership = user?.memberships.find((item) => item.id === communityId);
  const canManage = membership?.roles.includes("COMMUNITY_ADMIN") ?? false;

  const unit = useUnit(communityId, unitId);
  const ownerships = useOwnerships(communityId, unitId);
  const tenancies = useTenancies(communityId, unitId);
  const members = useMembers(communityId, canManage);
  const endOwnership = useEndOwnership(communityId, unitId);
  const revokeTenancy = useRevokeTenancy(communityId, unitId);
  const language: Language = i18n.language.startsWith("ca") ? "ca" : "es";

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  if (unit.isLoading) {
    return <FullPageLoader />;
  }

  if (unit.isError || !unit.data) {
    const forbidden = unit.error instanceof ApiError && unit.error.status === 403;
    return (
      <div className="page">
        <Alert variant="error">
          {forbidden ? t("common.accessDenied") : t("errors.notFound")}
        </Alert>
      </div>
    );
  }

  const activeMemberList = (members.data ?? []).filter((member) => member.status === "ACTIVE");
  const ownershipList = ownerships.data ?? [];
  const tenancyList = tenancies.data ?? [];
  const activeShare = sumActiveShares(activeOwnerships(ownershipList));

  return (
    <div className="page">
      <Link className="text-link" to={`/communities/${communityId}/structure`}>
        {t("unitDetail.back")}
      </Link>

      <header className="page-header">
        <div>
          <h1>{t("unitDetail.title", { code: unit.data.code })}</h1>
          <p className="page-subtitle">{t("unitDetail.subtitle")}</p>
        </div>
        <span className={`badge ${unit.data.status === "ACTIVE" ? "" : "badge--muted"}`}>
          {t(`structure.unitStatus.${unit.data.status}`, { defaultValue: unit.data.status })}
        </span>
      </header>

      <section className="detail-meta">
        <div>
          <span>{t("unitDetail.building")}</span>
          <strong>{unit.data.buildingName ?? t("structure.none")}</strong>
        </div>
        <div>
          <span>{t("unitDetail.type")}</span>
          <strong>{t(`structure.unitType.${unit.data.type}`, { defaultValue: unit.data.type })}</strong>
        </div>
        <div>
          <span>{t("unitDetail.coefficient")}</span>
          <strong>{formatPercent(Number(unit.data.coefficient), language)}</strong>
        </div>
        <div>
          <span>{t("unitDetail.activeShare")}</span>
          <strong>{formatPercent(activeShare, language)}</strong>
        </div>
      </section>

      <section className="structure-section">
        <div className="section-heading">
          <h2>{t("unitDetail.ownerships")}</h2>
          <span className="badge">{formatPercent(activeShare, language)}</span>
        </div>

        {endOwnership.isError ? <Alert variant="error">{t("unitDetail.error")}</Alert> : null}
        {ownerships.isError ? (
          <Alert variant="error">{t("common.accessDenied")}</Alert>
        ) : ownershipList.length === 0 ? (
          <div className="empty-state">{t("unitDetail.noOwnerships")}</div>
        ) : (
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>{t("unitDetail.member")}</th>
                  <th>{t("structure.fields.type")}</th>
                  <th>{t("unitDetail.share")}</th>
                  <th>{t("unitDetail.from")} / {t("unitDetail.to")}</th>
                  <th>{t("unitDetail.status")}</th>
                  {canManage ? <th>{t("structure.fields.actions")}</th> : null}
                </tr>
              </thead>
              <tbody>
                {ownershipList.map((ownership) => (
                  <tr key={ownership.id}>
                    <td data-label={t("unitDetail.member")}>
                      {ownership.fullName ?? ownership.email ?? ownership.userId}
                    </td>
                    <td data-label={t("structure.fields.type")}>
                      {t(`ownershipType.${ownership.ownershipType}`, { defaultValue: ownership.ownershipType })}
                    </td>
                    <td data-label={t("unitDetail.share")}>
                      {formatPercent(Number(ownership.sharePercentage), language)}
                    </td>
                    <td data-label={t("unitDetail.from")}>{formatPeriod(ownership.validFrom, ownership.validTo, language)}</td>
                    <td data-label={t("unitDetail.status")}>
                      <span className={`badge ${ownership.active ? "" : "badge--muted"}`}>
                        {ownership.active ? t("unitDetail.active") : t("unitDetail.inactive")}
                      </span>
                    </td>
                    {canManage ? (
                      <td data-label={t("structure.fields.actions")}>
                        {ownership.active ? (
                          <Button
                            variant="ghost"
                            className="btn--small"
                            onClick={() => endOwnership.mutate(ownership.id)}
                            loading={endOwnership.isPending}
                          >
                            {t("unitDetail.end")}
                          </Button>
                        ) : null}
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {canManage ? (
          <OwnershipForm communityId={communityId!} unitId={unitId!} members={activeMemberList} />
        ) : null}
      </section>

      <section className="structure-section">
        <div className="section-heading">
          <h2>{t("unitDetail.tenancies")}</h2>
        </div>

        {revokeTenancy.isError ? <Alert variant="error">{t("unitDetail.error")}</Alert> : null}
        {tenancies.isError ? (
          <Alert variant="error">{t("common.accessDenied")}</Alert>
        ) : tenancyList.length === 0 ? (
          <div className="empty-state">{t("unitDetail.noTenancies")}</div>
        ) : (
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>{t("unitDetail.member")}</th>
                  <th>{t("unitDetail.from")} / {t("unitDetail.to")}</th>
                  <th>{t("unitDetail.status")}</th>
                  {canManage ? <th>{t("structure.fields.actions")}</th> : null}
                </tr>
              </thead>
              <tbody>
                {tenancyList.map((tenancy) => (
                  <tr key={tenancy.id}>
                    <td data-label={t("unitDetail.member")}>
                      {tenancy.fullName ?? tenancy.email ?? tenancy.userId}
                    </td>
                    <td data-label={t("unitDetail.from")}>{formatPeriod(tenancy.validFrom, tenancy.validTo, language)}</td>
                    <td data-label={t("unitDetail.status")}>
                      <span className={`badge ${tenancy.active ? "" : "badge--muted"}`}>
                        {t(`tenancyStatus.${tenancy.status}`, { defaultValue: tenancy.status })}
                      </span>
                    </td>
                    {canManage ? (
                      <td data-label={t("structure.fields.actions")}>
                        {tenancy.status === "ACTIVE" ? (
                          <Button
                            variant="ghost"
                            className="btn--small"
                            onClick={() => revokeTenancy.mutate(tenancy.id)}
                            loading={revokeTenancy.isPending}
                          >
                            {t("unitDetail.revoke")}
                          </Button>
                        ) : null}
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {canManage ? (
          <TenancyForm communityId={communityId!} unitId={unitId!} members={activeMemberList} />
        ) : null}
      </section>
    </div>
  );
}

function formatPeriod(from: string, to: string | null | undefined, language: Language): string {
  return `${formatDate(from, language)} – ${to ? formatDate(to, language) : "…"}`;
}

function OwnershipForm({
  communityId,
  unitId,
  members,
}: {
  communityId: string;
  unitId: string;
  members: Member[];
}) {
  const { t } = useTranslation();
  const mutation = useCreateOwnership(communityId, unitId);
  const [userId, setUserId] = useState("");
  const [ownershipType, setOwnershipType] = useState<OwnershipType>("OWNER");
  const [sharePercentage, setSharePercentage] = useState("");
  const [validFrom, setValidFrom] = useState(today());
  const [validTo, setValidTo] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({
        userId,
        ownershipType,
        sharePercentage,
        validFrom,
        validTo: validTo || undefined,
      });
      setUserId("");
      setSharePercentage("");
      setValidTo("");
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("unitDetail.addOwnership")}</h3>
      {mutation.isError ? <Alert variant="error">{ownershipErrorMessage(mutation.error, t)}</Alert> : null}
      <div className="form-grid">
        <Select label={t("unitDetail.member")} value={userId} onChange={(e) => setUserId(e.target.value)} required>
          <option value="">{t("unitDetail.selectMember")}</option>
          {members.map((member) => (
            <option value={member.userId} key={member.userId}>
              {member.fullName} · {member.email}
            </option>
          ))}
        </Select>
        <Select
          label={t("structure.fields.type")}
          value={ownershipType}
          onChange={(e) => setOwnershipType(e.target.value as OwnershipType)}
        >
          {(["OWNER", "CO_OWNER", "USUFRUCTUARY", "NAKED_OWNER"] as OwnershipType[]).map((type) => (
            <option value={type} key={type}>
              {t(`ownershipType.${type}`)}
            </option>
          ))}
        </Select>
        <Input
          label={t("unitDetail.share")}
          type="number"
          min="0.0001"
          max="100"
          step="0.0001"
          value={sharePercentage}
          onChange={(e) => setSharePercentage(e.target.value)}
          required
        />
        <Input
          label={t("unitDetail.from")}
          type="date"
          value={validFrom}
          onChange={(e) => setValidFrom(e.target.value)}
          required
        />
        <Input label={t("unitDetail.to")} type="date" value={validTo} onChange={(e) => setValidTo(e.target.value)} />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>
          {t("common.save")}
        </Button>
      </div>
    </form>
  );
}

function TenancyForm({
  communityId,
  unitId,
  members,
}: {
  communityId: string;
  unitId: string;
  members: Member[];
}) {
  const { t } = useTranslation();
  const mutation = useCreateTenancy(communityId, unitId);
  const [userId, setUserId] = useState("");
  const [validFrom, setValidFrom] = useState(today());
  const [validTo, setValidTo] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({ userId, validFrom, validTo: validTo || undefined });
      setUserId("");
      setValidTo("");
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("unitDetail.addTenancy")}</h3>
      {mutation.isError ? <Alert variant="error">{tenancyErrorMessage(mutation.error, t)}</Alert> : null}
      <div className="form-grid">
        <Select label={t("unitDetail.member")} value={userId} onChange={(e) => setUserId(e.target.value)} required>
          <option value="">{t("unitDetail.selectMember")}</option>
          {members.map((member) => (
            <option value={member.userId} key={member.userId}>
              {member.fullName} · {member.email}
            </option>
          ))}
        </Select>
        <Input
          label={t("unitDetail.from")}
          type="date"
          value={validFrom}
          onChange={(e) => setValidFrom(e.target.value)}
          required
        />
        <Input label={t("unitDetail.to")} type="date" value={validTo} onChange={(e) => setValidTo(e.target.value)} />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>
          {t("common.save")}
        </Button>
      </div>
    </form>
  );
}

function ownershipErrorMessage(error: unknown, t: (key: string) => string): string {
  if (error instanceof ApiError) {
    if (error.code === "OWNERSHIP_SUM_EXCEEDS") {
      return t("unitDetail.ownershipSumExceeds");
    }
    if (error.code === "OWNERSHIP_ALREADY_ACTIVE") {
      return t("unitDetail.ownershipDuplicate");
    }
    if (error.code === "OWNERSHIP_USER_NOT_MEMBER") {
      return t("unitDetail.userNotMember");
    }
  }
  return t("unitDetail.error");
}

function tenancyErrorMessage(error: unknown, t: (key: string) => string): string {
  if (error instanceof ApiError) {
    if (error.code === "TENANCY_ALREADY_ACTIVE") {
      return t("unitDetail.tenancyDuplicate");
    }
    if (error.code === "TENANCY_USER_NOT_MEMBER") {
      return t("unitDetail.userNotMember");
    }
  }
  return t("unitDetail.error");
}

import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { ShieldCheck, ShieldOff } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { Textarea } from "../components/Textarea";
import {
  useAuthorizations,
  useCreateAuthorization,
  useRevokeAuthorization,
  type AuthorizationDirection,
} from "../features/authorizations/queries";
import { useAuthStore } from "../features/auth/authStore";
import { useMembers } from "../features/communities/queries";
import {
  canCastVotes,
  canCreateAuthorizations,
  canCreateWorkPermits,
  canManageAuthorizations,
  isReadOnly,
} from "../features/mvp/utils";
import type { Authorization, AuthorizationScope, Role } from "../types/api";
import { formatDate } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

function availableScopes(roles: Role[]): AuthorizationScope[] {
  const scopes: AuthorizationScope[] = [];
  if (canCastVotes(roles)) scopes.push("VOTE");
  if (canCreateWorkPermits(roles)) scopes.push("WORK_PERMIT");
  if (!isReadOnly(roles)) scopes.push("INCIDENT", "BOARD", "LOST_FOUND");
  return scopes;
}

export function AuthorizationsPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const roles = user?.memberships.find((item) => item.id === communityId)?.roles ?? [];
  const language = normalizeLanguage(i18n.language);
  const canManage = canManageAuthorizations(roles);
  const canCreate = canCreateAuthorizations(roles);
  const scopes = useMemo(() => availableScopes(roles), [roles]);
  const [direction, setDirection] = useState<AuthorizationDirection>("GRANTED");
  const authorizations = useAuthorizations(communityId, direction, direction === "ALL" && canManage);
  const members = useMembers(communityId, canCreate);
  const create = useCreateAuthorization(communityId);
  const revoke = useRevokeAuthorization(communityId);
  const [granteeId, setGranteeId] = useState("");
  const [scope, setScope] = useState<AuthorizationScope | "">(scopes[0] ?? "");
  const [validFrom, setValidFrom] = useState(() => new Date().toISOString().slice(0, 10));
  const [validUntil, setValidUntil] = useState("");
  const [notes, setNotes] = useState("");

  useEffect(() => {
    setScope((current) => (scopes.includes(current as AuthorizationScope) ? current : scopes[0] ?? ""));
  }, [scopes]);

  const granteeOptions = (members.data ?? []).filter(
    (member) => member.status === "ACTIVE" && member.userId !== user?.id,
  );

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!granteeId || !scope) {
      return;
    }
    try {
      await create.mutateAsync({
        granteeMembershipId: granteeId,
        scope,
        validFrom: validFrom || undefined,
        validUntil: validUntil || undefined,
        notes: notes || undefined,
      });
      setNotes("");
    } catch {
      // el error se muestra en el aviso
    }
  }

  async function revokeAuthorization(authorization: Authorization) {
    if (!window.confirm(t("authorizations.revokeConfirm"))) {
      return;
    }
    try {
      await revoke.mutateAsync(authorization.id);
    } catch {
      // el error se muestra en el aviso
    }
  }

  function statusLabel(authorization: Authorization): string {
    if (authorization.status === "REVOKED") {
      return t("authorizations.status.REVOKED");
    }
    return authorization.effective
      ? t("authorizations.status.EFFECTIVE")
      : t("authorizations.status.EXPIRED");
  }

  function canRevoke(authorization: Authorization): boolean {
    return authorization.status === "ACTIVE"
      && (canManage || authorization.grantorUserId === user?.id);
  }

  if (authorizations.isLoading) {
    return <FullPageLoader />;
  }

  const tabs: AuthorizationDirection[] = canManage
    ? ["GRANTED", "RECEIVED", "ALL"]
    : ["GRANTED", "RECEIVED"];

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{t("authorizations.title")}</h1>
          <p className="page-subtitle">{t("authorizations.subtitle")}</p>
        </div>
      </header>

      {roles.includes("VECINO") ? (
        <Alert variant="info">{t("authorizations.sharedWarning")}</Alert>
      ) : null}
      {!canCastVotes(roles) ? (
        <Alert variant="info">{t("authorizations.voteRequiresIndividual")}</Alert>
      ) : null}

      {canCreate && scopes.length ? (
        <form className="form-card" onSubmit={submit}>
          <h3>{t("authorizations.new")}</h3>
          <p className="page-subtitle">{t("authorizations.newHint")}</p>
          <p className="field__hint">{t("authorizations.scopeHint")}</p>
          <div className="form-grid">
            <Select
              label={t("authorizations.grantee")}
              value={granteeId}
              onChange={(event) => setGranteeId(event.target.value)}
              required
            >
              <option value="">{t("authorizations.granteePlaceholder")}</option>
              {granteeOptions.map((member) => (
                <option value={member.membershipId} key={member.membershipId}>
                  {member.fullName}
                </option>
              ))}
            </Select>
            <Select
              label={t("authorizations.scope")}
              value={scope}
              onChange={(event) => setScope(event.target.value as AuthorizationScope)}
            >
              {scopes.map((value) => (
                <option value={value} key={value}>
                  {t(`authorizations.scopes.${value}`)}
                </option>
              ))}
            </Select>
            <Input
              label={t("authorizations.validFrom")}
              type="date"
              value={validFrom}
              onChange={(event) => setValidFrom(event.target.value)}
            />
            <Input
              label={t("authorizations.validUntil")}
              type="date"
              value={validUntil}
              onChange={(event) => setValidUntil(event.target.value)}
            />
          </div>
          <Textarea
            label={t("authorizations.notes")}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            rows={2}
            placeholder={t("authorizations.notesPlaceholder")}
          />
          {create.isError ? <Alert variant="error">{t("authorizations.error")}</Alert> : null}
          <div className="form-actions">
            <Button type="submit" loading={create.isPending} disabled={!granteeId || !scope}>
              {t("authorizations.create")}
            </Button>
          </div>
        </form>
      ) : null}

      <div className="tabs" role="tablist">
        {tabs.map((value) => (
          <button
            type="button"
            key={value}
            role="tab"
            aria-selected={direction === value}
            className={`tab ${direction === value ? "tab--active" : ""}`}
            onClick={() => setDirection(value)}
          >
            {t(`authorizations.tabs.${value}`)}
          </button>
        ))}
      </div>

      {authorizations.isError ? (
        <Alert variant="error">{t("authorizations.error")}</Alert>
      ) : null}
      {revoke.isError ? <Alert variant="error">{t("authorizations.error")}</Alert> : null}

      {authorizations.data?.length ? (
        <div className="authorization-grid">
          {authorizations.data.map((authorization) => (
            <article className="authorization-card" key={authorization.id}>
              <header className="authorization-card__head">
                <span className="authorization-card__scope">
                  <ShieldCheck size={15} aria-hidden="true" />
                  {t(`authorizations.scopes.${authorization.scope}`)}
                </span>
                <span className={`authorization-card__status authorization-card__status--${authorization.effective ? "active" : "inactive"}`}>
                  {statusLabel(authorization)}
                </span>
              </header>
              <p className="authorization-card__people">
                <strong>{authorization.grantorName ?? "—"}</strong>
                <span aria-hidden="true">→</span>
                <strong>{authorization.granteeName ?? "—"}</strong>
              </p>
              <dl className="authorization-card__meta">
                <div>
                  <dt>{t("authorizations.validFrom")}</dt>
                  <dd>{formatDate(new Date(`${authorization.validFrom}T00:00:00`), language)}</dd>
                </div>
                <div>
                  <dt>{t("authorizations.validUntil")}</dt>
                  <dd>{authorization.validUntil
                    ? formatDate(new Date(`${authorization.validUntil}T00:00:00`), language)
                    : t("authorizations.noExpiry")}</dd>
                </div>
              </dl>
              {authorization.notes ? <p className="authorization-card__notes">{authorization.notes}</p> : null}
              {canRevoke(authorization) ? (
                <Button type="button" variant="ghost" onClick={() => void revokeAuthorization(authorization)}>
                  <ShieldOff size={14} aria-hidden="true" /> {t("authorizations.revoke")}
                </Button>
              ) : null}
            </article>
          ))}
        </div>
      ) : (
        <div className="empty-state">{t("authorizations.empty")}</div>
      )}
    </div>
  );
}

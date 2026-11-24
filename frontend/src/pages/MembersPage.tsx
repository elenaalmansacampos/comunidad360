import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { UserPlus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { useAuthStore } from "../features/auth/authStore";
import { useMembers } from "../features/communities/queries";
import { useCommunityStore } from "../features/communities/communityStore";
import { useGenerateVecinoAccess, useInviteMember } from "../features/members/queries";
import { ApiError } from "../services/apiClient";
import type { Role, VecinoAccess } from "../types/api";

const INVITE_ROLES: Role[] = [
  "VIEWER",
  "OWNER",
  "TENANT",
  "TREASURER",
  "SECRETARY",
  "PRESIDENT",
  "COMMUNITY_ADMIN",
];

function roleLabel(role: Role): string {
  return `roles.${role}`;
}

export function MembersPage() {
  const { t } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const members = useMembers(communityId);
  const invite = useInviteMember(communityId);
  const roles = user?.memberships.find((item) => item.id === communityId)?.roles ?? [];
  const canInvite = roles.some((role) =>
    ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY"].includes(role));
  const isAdmin = roles.includes("COMMUNITY_ADMIN");
  const vecino = useGenerateVecinoAccess(communityId);
  const [showInvite, setShowInvite] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<Role>("VIEWER");
  const [inviteUrl, setInviteUrl] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [vecinoAccess, setVecinoAccess] = useState<VecinoAccess | null>(null);
  const [vecinoCopied, setVecinoCopied] = useState(false);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  async function submitInvite(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCopied(false);
    try {
      const result = await invite.mutateAsync({ email, role });
      if (result.devToken) {
        const base = import.meta.env.BASE_URL.endsWith("/")
          ? import.meta.env.BASE_URL
          : `${import.meta.env.BASE_URL}/`;
        setInviteUrl(`${window.location.origin}${base}invitation/${result.devToken}`);
      } else {
        setInviteUrl(null);
      }
      setEmail("");
    } catch {
      // estado de la mutación
    }
  }

  async function generateVecino() {
    setVecinoCopied(false);
    try {
      const result = await vecino.mutateAsync();
      setVecinoAccess(result);
    } catch {
      // estado de la mutación
    }
  }

  async function copyVecino() {
    if (!vecinoAccess) {
      return;
    }
    try {
      await navigator.clipboard.writeText(`${vecinoAccess.email}\n${vecinoAccess.password}`);
      setVecinoCopied(true);
      window.setTimeout(() => setVecinoCopied(false), 1500);
    } catch {
      setVecinoCopied(false);
    }
  }

  async function copyLink() {
    if (!inviteUrl) {
      return;
    }
    try {
      await navigator.clipboard.writeText(inviteUrl);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      setCopied(false);
    }
  }

  if (members.isLoading) {
    return <FullPageLoader />;
  }

  if (members.isError) {
    const forbidden = members.error instanceof ApiError && members.error.status === 403;
    return (
      <div className="page">
        <Alert variant="error">
          {forbidden ? t("members.forbidden") : t("common.unknownError")}
        </Alert>
        {!forbidden ? (
          <Button variant="secondary" onClick={() => void members.refetch()}>
            {t("common.retry")}
          </Button>
        ) : null}
      </div>
    );
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{t("members.title")}</h1>
          <p className="page-subtitle">{t("members.subtitle")}</p>
        </div>
        {canInvite ? (
          <Button onClick={() => setShowInvite((value) => !value)}>
            <UserPlus size={16} aria-hidden="true" /> {t("members.invite")}
          </Button>
        ) : null}
      </header>

      {showInvite && canInvite ? (
        <form className="form-card" onSubmit={submitInvite}>
          <h3>{t("members.invite")}</h3>
          {invite.isError ? <Alert variant="error">{t("members.inviteError")}</Alert> : null}
          {inviteUrl ? (
            <>
              <Alert variant="success">{t("members.inviteSent")}</Alert>
              <div className="invite-link">
                <code>{inviteUrl}</code>
                <Button type="button" variant="secondary" onClick={() => void copyLink()}>
                  {copied ? t("members.copied") : t("members.copyLink")}
                </Button>
              </div>
            </>
          ) : null}
          <div className="form-grid">
            <Input
              label={t("members.inviteEmail")}
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
            <Select
              label={t("members.inviteRole")}
              value={role}
              onChange={(event) => setRole(event.target.value as Role)}
            >
              {INVITE_ROLES.map((value) => (
                <option value={value} key={value}>
                  {t(roleLabel(value), { defaultValue: value })}
                </option>
              ))}
            </Select>
          </div>
          <p className="field__hint">{t("members.inviteRoleHint")}</p>
          <div className="form-actions">
            <Button type="submit" loading={invite.isPending}>{t("members.inviteSubmit")}</Button>
          </div>
        </form>
      ) : null}

      {isAdmin ? (
        <section className="form-card vecino-access-card">
          <h3>{t("members.vecinoAccess")}</h3>
          <p className="page-subtitle">{t("members.vecinoAccessHint")}</p>
          <p className="muted">{t("members.vecinoAuditNote")}</p>
          {vecino.isError ? <Alert variant="error">{t("members.vecinoAccessError")}</Alert> : null}
          <div className="form-actions">
            <Button onClick={() => void generateVecino()} loading={vecino.isPending}>
              {t("members.generateVecino")}
            </Button>
          </div>
          {vecinoAccess ? (
            <>
              <Alert variant="info">{t("members.vecinoAccessWarning")}</Alert>
              <div className="invite-link">
                <code>{t("members.vecinoEmail")}: {vecinoAccess.email}</code>
                <code>{t("members.vecinoPassword")}: {vecinoAccess.password}</code>
                <Button type="button" variant="secondary" onClick={() => void copyVecino()}>
                  {vecinoCopied ? t("members.copied") : t("members.copyCredentials")}
                </Button>
              </div>
            </>
          ) : null}
        </section>
      ) : null}

      {members.data?.length === 0 ? (
        <div className="empty-state">{t("members.empty")}</div>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("members.name")}</th>
                <th>{t("members.email")}</th>
                <th>{t("members.status")}</th>
                <th>{t("members.roles")}</th>
              </tr>
            </thead>
            <tbody>
              {members.data?.map((member) => (
                <tr key={member.membershipId}>
                  <td data-label={t("members.name")}>{member.fullName}</td>
                  <td data-label={t("members.email")}>{member.email}</td>
                  <td data-label={t("members.status")}>
                    <span className="badge badge--muted">
                      {t(`statuses.${member.status}`, { defaultValue: member.status })}
                    </span>
                  </td>
                  <td data-label={t("members.roles")}>
                    <div className="chips">
                      {member.roles.map((value) => (
                        <span className="badge" key={value}>
                          {t(roleLabel(value), { defaultValue: value })}
                        </span>
                      ))}
                    </div>
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

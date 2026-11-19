import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { useMembers } from "../features/communities/queries";
import { useCommunityStore } from "../features/communities/communityStore";
import { ApiError } from "../services/apiClient";
import type { Role } from "../types/api";

function roleLabel(role: Role): string {
  return `roles.${role}`;
}

export function MembersPage() {
  const { t } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const members = useMembers(communityId);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

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
      </header>

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
                      {member.roles.map((role) => (
                        <span className="badge" key={role}>
                          {t(roleLabel(role), { defaultValue: role })}
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

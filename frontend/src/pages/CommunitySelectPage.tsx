import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunities } from "../features/communities/queries";
import { useCommunityStore } from "../features/communities/communityStore";
import type { Role } from "../types/api";

function roleLabel(role: Role): string {
  return `roles.${role}`;
}

export function CommunitySelectPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const communities = useCommunities();

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  function selectCommunity(communityId: string) {
    setSelectedCommunityId(communityId);
    navigate(`/communities/${communityId}/dashboard`);
  }

  return (
    <div className="select-shell">
      <header className="select-header">
        <span className="brand">COMUNIDAD 360</span>
        <div className="select-header__right">
          <LanguageSwitcher />
          {user ? <span className="app-header__user">{user.fullName}</span> : null}
          <button type="button" className="btn btn--ghost btn--small" onClick={handleLogout}>
            {t("common.logout")}
          </button>
        </div>
      </header>

      <main className="select-main">
        <h1>{t("communities.title")}</h1>
        <p className="page-subtitle">{t("communities.subtitle")}</p>

        {communities.isLoading ? <FullPageLoader /> : null}
        {communities.isError ? (
          <Alert variant="error">
            {t("common.networkError")}{" "}
            <Button variant="ghost" onClick={() => void communities.refetch()}>
              {t("common.retry")}
            </Button>
          </Alert>
        ) : null}

        {communities.data?.length === 0 ? (
          <div className="empty-state">{t("communities.empty")}</div>
        ) : null}

        <div className="community-list">
          {communities.data?.map((community) => (
            <article className="community-card" key={community.id}>
              <div>
                <h2>{community.name}</h2>
                <div className="chips">
                  {community.roles.map((role) => (
                    <span className="badge" key={role}>
                      {t(roleLabel(role), { defaultValue: role })}
                    </span>
                  ))}
                </div>
              </div>
              <Button onClick={() => selectCommunity(community.id)}>{t("communities.select")}</Button>
            </article>
          ))}
        </div>
      </main>
    </div>
  );
}

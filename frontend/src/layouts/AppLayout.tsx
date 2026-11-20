import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, NavLink, Outlet, useLocation, useNavigate, useParams } from "react-router-dom";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { NotificationBell } from "../components/NotificationBell";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunities } from "../features/communities/queries";
import { useCommunityStore } from "../features/communities/communityStore";
import { canViewProviders } from "../features/mvp/utils";
import { demoEnabled } from "../services/demoApi";

export function AppLayout() {
  const { t } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const [moreOpen, setMoreOpen] = useState(false);
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const communities = useCommunities();
  const community = communities.data?.find((item) => item.id === communityId);
  const roles = user?.memberships.find((item) => item.id === communityId)?.roles ?? [];

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  useEffect(() => {
    setMoreOpen(false);
  }, [location.pathname]);

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="app-shell">
      {demoEnabled ? <div className="demo-banner">{t("demo.banner")}</div> : null}
      <header className="app-header">
        <div className="app-header__left">
          <span className="brand">COMUNIDAD 360</span>
          <Link className="app-header__community" to="/communities">{community?.name ?? t("common.loading")}</Link>
        </div>
        <div className="app-header__right">
          <NotificationBell />
          <LanguageSwitcher />
          {user ? <span className="app-header__user">{user.fullName}</span> : null}
          <button type="button" className="btn btn--ghost btn--small" onClick={handleLogout}>
            {t("common.logout")}
          </button>
        </div>
      </header>

      <div className="app-body">
        <aside className="sidebar" aria-label={t("nav.community")}>
          <nav className="sidebar__nav">
            <NavLink
              to="/communities"
              end
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.community")}
            </NavLink>
            <NavLink
              to={`/communities/${communityId}/dashboard`}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.home")}
            </NavLink>
            <NavLink
              to={`/communities/${communityId}/structure`}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.structure")}
            </NavLink>
            <NavLink
              to={`/communities/${communityId}/finance`}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.finance")}
            </NavLink>
            <NavLink
              to={`/communities/${communityId}/banking`}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.banking")}
            </NavLink>
            <NavLink
              to={`/communities/${communityId}/incidents`}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.incidents")}
            </NavLink>
            <NavLink
              to={`/communities/${communityId}/meetings`}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.meetings")}
            </NavLink>
            <NavLink
              to={`/communities/${communityId}/documents`}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.documents")}
            </NavLink>
            <NavLink
              to={`/communities/${communityId}/chat`}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.chat")}
            </NavLink>
            {canViewProviders(roles) ? (
              <NavLink
                to={`/communities/${communityId}/providers`}
                className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
              >
                {t("nav.providers")}
              </NavLink>
            ) : null}
            <NavLink
              to={`/communities/${communityId}/votings`}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.votings")}
            </NavLink>
            <NavLink
              to={`/communities/${communityId}/members`}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
            >
              {t("nav.members")}
            </NavLink>
          </nav>
        </aside>

        <main className="app-content">
          <Outlet />
        </main>
      </div>

      {moreOpen ? (
        <>
          <button
            type="button"
            className="mobile-more__backdrop"
            aria-label={t("common.cancel")}
            onClick={() => setMoreOpen(false)}
          />
          <nav className="mobile-more" id="mobile-more" aria-label={t("nav.more")}>
            <div className="mobile-more__header">
              <strong>{t("nav.more")}</strong>
              <button type="button" className="text-link text-link--button" onClick={() => setMoreOpen(false)}>
                {t("common.cancel")}
              </button>
            </div>
            <div className="mobile-more__grid">
              <NavLink to={`/communities/${communityId}/banking`}>{t("nav.banking")}</NavLink>
              <NavLink to={`/communities/${communityId}/meetings`}>{t("nav.meetings")}</NavLink>
              <NavLink to={`/communities/${communityId}/documents`}>{t("nav.documents")}</NavLink>
              <NavLink to={`/communities/${communityId}/chat`}>{t("nav.chat")}</NavLink>
              {canViewProviders(roles) ? (
                <NavLink to={`/communities/${communityId}/providers`}>{t("nav.providers")}</NavLink>
              ) : null}
              <NavLink to={`/communities/${communityId}/votings`}>{t("nav.votings")}</NavLink>
              <NavLink to={`/communities/${communityId}/members`}>{t("nav.members")}</NavLink>
              <NavLink to={`/communities/${communityId}/notification-preferences`}>
                {t("notifications.preferences")}
              </NavLink>
            </div>
          </nav>
        </>
      ) : null}

      <nav className="mobile-nav" aria-label={t("nav.more")}>
        <NavLink
          to={`/communities/${communityId}/dashboard`}
          className={({ isActive }) => `mobile-nav__item ${isActive ? "mobile-nav__item--active" : ""}`}
        >
          {t("nav.home")}
        </NavLink>
        <NavLink
          to={`/communities/${communityId}/structure`}
          className={({ isActive }) => `mobile-nav__item ${isActive ? "mobile-nav__item--active" : ""}`}
        >
          {t("nav.structure")}
        </NavLink>
        <NavLink
          to={`/communities/${communityId}/finance`}
          className={({ isActive }) => `mobile-nav__item ${isActive ? "mobile-nav__item--active" : ""}`}
        >
          {t("nav.finance")}
        </NavLink>
        <NavLink
          to={`/communities/${communityId}/incidents`}
          className={({ isActive }) => `mobile-nav__item ${isActive ? "mobile-nav__item--active" : ""}`}
        >
          {t("nav.incidents")}
        </NavLink>
        <button
          type="button"
          className={`mobile-nav__item mobile-nav__item--button ${moreOpen ? "mobile-nav__item--active" : ""}`}
          aria-expanded={moreOpen}
          aria-controls="mobile-more"
          onClick={() => setMoreOpen((value) => !value)}
        >
          {t("nav.more")}
        </button>
      </nav>
    </div>
  );
}

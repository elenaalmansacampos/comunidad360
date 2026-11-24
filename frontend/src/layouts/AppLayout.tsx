import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import {
  AlertTriangle,
  Building2,
  CalendarDays,
  ChevronDown,
  FileText,
  Landmark,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageCircle,
  PackageSearch,
  StickyNote,
  Users,
  Vote,
  Wallet,
  Wrench,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link, NavLink, Outlet, useLocation, useNavigate, useParams } from "react-router-dom";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { NotificationBell } from "../components/NotificationBell";
import { ThemeToggle } from "../components/ThemeToggle";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunities } from "../features/communities/queries";
import { useCommunityStore } from "../features/communities/communityStore";
import { canViewProviders } from "../features/mvp/utils";
import { demoEnabled } from "../services/demoApi";

function initials(name: string | undefined): string {
  if (!name) {
    return "?";
  }
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function NavItem({ to, icon, label, end }: { to: string; icon: ReactNode; label: string; end?: boolean }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
    >
      <span className="nav-link__icon" aria-hidden="true">{icon}</span>
      <span>{label}</span>
    </NavLink>
  );
}

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
  const roleLabel = roles[0] ? t(`roles.${roles[0]}`, { defaultValue: roles[0] }) : "";

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
          <Link className="brand brand--link" to="/communities" aria-label={t("common.appName")}>
            <span className="brand__mark" aria-hidden="true">
              <Building2 size={18} />
            </span>
            <span>COMUNIDAD 360</span>
          </Link>
          <Link className="app-header__community" to="/communities">
            <span>{community?.name ?? t("common.loading")}</span>
            <ChevronDown size={14} aria-hidden="true" />
          </Link>
        </div>
        <div className="app-header__right">
          <NotificationBell />
          <LanguageSwitcher />
          <ThemeToggle />
          {user ? (
            <span className="app-header__user">
              <span className="avatar avatar--sm" aria-hidden="true">{initials(user.fullName)}</span>
              <span className="app-header__user-name">{user.fullName}</span>
            </span>
          ) : null}
          <button
            type="button"
            className="icon-btn"
            onClick={handleLogout}
            aria-label={t("common.logout")}
            title={t("common.logout")}
          >
            <LogOut size={18} aria-hidden="true" />
          </button>
        </div>
      </header>

      <div className="app-body">
        <aside className="sidebar" aria-label={t("nav.community")}>
          <div className="sidebar__community">
            <span className="sidebar__community-icon" aria-hidden="true"><Building2 size={16} /></span>
            <div>
              <small>{t("nav.community")}</small>
              <strong>{community?.name ?? t("common.loading")}</strong>
            </div>
          </div>
          <nav className="sidebar__nav">
            <p className="nav-group">{t("nav.groups.general")}</p>
            <NavItem to="/communities" end icon={<Building2 size={18} />} label={t("nav.community")} />
            <NavItem
              to={`/communities/${communityId}/dashboard`}
              icon={<LayoutDashboard size={18} />}
              label={t("nav.home")}
            />
            <NavItem
              to={`/communities/${communityId}/structure`}
              icon={<Building2 size={18} />}
              label={t("nav.structure")}
            />
            <p className="nav-group">{t("nav.groups.management")}</p>
            <NavItem to={`/communities/${communityId}/finance`} icon={<Wallet size={18} />} label={t("nav.finance")} />
            <NavItem to={`/communities/${communityId}/banking`} icon={<Landmark size={18} />} label={t("nav.banking")} />
            <NavItem to={`/communities/${communityId}/incidents`} icon={<AlertTriangle size={18} />} label={t("nav.incidents")} />
            <NavItem to={`/communities/${communityId}/meetings`} icon={<CalendarDays size={18} />} label={t("nav.meetings")} />
            <NavItem to={`/communities/${communityId}/documents`} icon={<FileText size={18} />} label={t("nav.documents")} />
            <p className="nav-group">{t("nav.groups.community")}</p>
            <NavItem to={`/communities/${communityId}/chat`} icon={<MessageCircle size={18} />} label={t("nav.chat")} />
            <NavItem to={`/communities/${communityId}/board`} icon={<StickyNote size={18} />} label={t("nav.board")} />
            <NavItem to={`/communities/${communityId}/lost-found`} icon={<PackageSearch size={18} />} label={t("nav.lostFound")} />
            {canViewProviders(roles) ? (
              <NavItem to={`/communities/${communityId}/providers`} icon={<Wrench size={18} />} label={t("nav.providers")} />
            ) : null}
            <NavItem to={`/communities/${communityId}/votings`} icon={<Vote size={18} />} label={t("nav.votings")} />
            <NavItem to={`/communities/${communityId}/members`} icon={<Users size={18} />} label={t("nav.members")} />
          </nav>
          {user ? (
            <div className="sidebar__footer">
              <div className="sidebar__user">
                <span className="avatar" aria-hidden="true">{initials(user.fullName)}</span>
                <div>
                  <strong>{user.fullName}</strong>
                  <small>{roleLabel}</small>
                </div>
              </div>
              <button
                type="button"
                className="icon-btn"
                onClick={handleLogout}
                aria-label={t("common.logout")}
                title={t("common.logout")}
              >
                <LogOut size={18} aria-hidden="true" />
              </button>
            </div>
          ) : null}
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
              <NavLink to={`/communities/${communityId}/board`}>{t("nav.board")}</NavLink>
              <NavLink to={`/communities/${communityId}/lost-found`}>{t("nav.lostFound")}</NavLink>
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
          <LayoutDashboard size={18} aria-hidden="true" />
          <span>{t("nav.home")}</span>
        </NavLink>
        <NavLink
          to={`/communities/${communityId}/structure`}
          className={({ isActive }) => `mobile-nav__item ${isActive ? "mobile-nav__item--active" : ""}`}
        >
          <Building2 size={18} aria-hidden="true" />
          <span>{t("nav.structure")}</span>
        </NavLink>
        <NavLink
          to={`/communities/${communityId}/finance`}
          className={({ isActive }) => `mobile-nav__item ${isActive ? "mobile-nav__item--active" : ""}`}
        >
          <Wallet size={18} aria-hidden="true" />
          <span>{t("nav.finance")}</span>
        </NavLink>
        <NavLink
          to={`/communities/${communityId}/incidents`}
          className={({ isActive }) => `mobile-nav__item ${isActive ? "mobile-nav__item--active" : ""}`}
        >
          <AlertTriangle size={18} aria-hidden="true" />
          <span>{t("nav.incidents")}</span>
        </NavLink>
        <button
          type="button"
          className={`mobile-nav__item mobile-nav__item--button ${moreOpen ? "mobile-nav__item--active" : ""}`}
          aria-expanded={moreOpen}
          aria-controls="mobile-more"
          onClick={() => setMoreOpen((value) => !value)}
        >
          <Menu size={18} aria-hidden="true" />
          <span>{t("nav.more")}</span>
        </button>
      </nav>
    </div>
  );
}

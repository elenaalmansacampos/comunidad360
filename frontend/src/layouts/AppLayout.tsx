import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import {
  AlertTriangle,
  BookUser,
  Building2,
  CalendarCheck,
  CalendarDays,
  ChevronDown,
  Eye,
  FileText,
  HardHat,
  Landmark,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageCircle,
  PackageSearch,
  StickyNote,
  UserCheck,
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
import { useReceivedAuthorizations } from "../features/authorizations/queries";
import { useDelegationStore } from "../features/authorizations/delegationStore";
import { useCommunities } from "../features/communities/queries";
import { useCommunityStore } from "../features/communities/communityStore";
import { canViewProviders, isReadOnly } from "../features/mvp/utils";
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

function NavItem({ to, icon, label, end, accent }: {
  to: string;
  icon: ReactNode;
  label: string;
  end?: boolean;
  accent?: string;
}) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        `nav-link ${isActive ? "nav-link--active" : ""} ${accent ? `nav-link--accent-${accent}` : ""}`}
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
  const isVecino = roles.includes("VECINO");
  const roleLabel = roles[0] ? t(`roles.${roles[0]}`, { defaultValue: roles[0] }) : "";
  const displayRoleLabel = isVecino ? t("roles.VECINO") : roleLabel;
  const readOnly = isReadOnly(roles);
  const receivedAuthorizations = useReceivedAuthorizations(communityId);
  const activeDelegation = useDelegationStore((state) => state.active);
  const setActiveDelegation = useDelegationStore((state) => state.setActive);
  const clearDelegation = useDelegationStore((state) => state.clear);
  const delegationOptions = (receivedAuthorizations.data ?? []).filter(
    (authorization) => authorization.effective && authorization.status === "ACTIVE",
  );
  const activeDelegationKey = activeDelegation
    ? `${activeDelegation.scope}:${activeDelegation.membershipId}`
    : "";

  useEffect(() => {
    if (activeDelegation && activeDelegation.communityId !== communityId) {
      clearDelegation();
    }
  }, [activeDelegation, clearDelegation, communityId]);

  useEffect(() => {
    if (activeDelegation && !delegationOptions.some((authorization) =>
      authorization.scope === activeDelegation.scope
      && authorization.grantorMembershipId === activeDelegation.membershipId)) {
      clearDelegation();
    }
  }, [activeDelegation, clearDelegation, delegationOptions]);

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

  const communityMoreLinks = [
    { to: `/communities/${communityId}/incidents`, label: t("nav.incidents"), community: true, accent: "rose" },
    { to: `/communities/${communityId}/reservations`, label: t("nav.reservations"), community: true, accent: "violet" },
    { to: `/communities/${communityId}/chat`, label: t("nav.chat"), community: true, accent: "blue" },
    { to: `/communities/${communityId}/board`, label: t("nav.board"), community: true, accent: "amber" },
    { to: `/communities/${communityId}/lost-found`, label: t("nav.lostFound"), community: true, accent: "teal" },
    { to: `/communities/${communityId}/votings`, label: t("nav.votings"), community: true, accent: "indigo" },
    { to: `/communities/${communityId}/members`, label: t("nav.members"), community: true, accent: "slate" },
    { to: `/communities/${communityId}/authorizations`, label: t("nav.authorizations"), community: true, accent: "cyan" },
    { to: `/communities/${communityId}/directory`, label: t("nav.directory"), community: true, accent: "green" },
  ];
  const managementMoreLinks = [
    ...(canViewProviders(roles)
      ? [{ to: `/communities/${communityId}/providers`, label: t("nav.providers"), community: false, accent: "" }]
      : []),
    { to: `/communities/${communityId}/banking`, label: t("nav.banking"), community: false, accent: "" },
    { to: `/communities/${communityId}/meetings`, label: t("nav.meetings"), community: false, accent: "" },
    { to: `/communities/${communityId}/documents`, label: t("nav.documents"), community: false, accent: "" },
    { to: `/communities/${communityId}/work-permits`, label: t("nav.workPermits"), community: false, accent: "" },
    { to: `/communities/${communityId}/notification-preferences`, label: t("notifications.preferences"), community: false, accent: "" },
  ];
  const orderedMobileLinks = isVecino
    ? [...communityMoreLinks, ...managementMoreLinks]
    : [...managementMoreLinks, ...communityMoreLinks];

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
          <div className={`sidebar__role ${isVecino ? "sidebar__role--vecino" : ""}`}>
            <strong>{displayRoleLabel}</strong>
          </div>
          <nav className={`sidebar__nav ${isVecino ? "sidebar__nav--vecino" : ""}`}>
            <div className="nav-section nav-section--general">
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
            </div>

            <div className="nav-section nav-section--management">
              <p className="nav-group">{t("nav.groups.management")}</p>
              <NavItem to={`/communities/${communityId}/finance`} icon={<Wallet size={18} />} label={t("nav.finance")} />
              <NavItem to={`/communities/${communityId}/banking`} icon={<Landmark size={18} />} label={t("nav.banking")} />
              {!isVecino ? (
                <>
                  <NavItem to={`/communities/${communityId}/incidents`} icon={<AlertTriangle size={18} />} label={t("nav.incidents")} />
                  <NavItem to={`/communities/${communityId}/reservations`} icon={<CalendarCheck size={18} />} label={t("nav.reservations")} />
                </>
              ) : null}
              <NavItem to={`/communities/${communityId}/meetings`} icon={<CalendarDays size={18} />} label={t("nav.meetings")} />
              <NavItem to={`/communities/${communityId}/documents`} icon={<FileText size={18} />} label={t("nav.documents")} />
              <NavItem to={`/communities/${communityId}/work-permits`} icon={<HardHat size={18} />} label={t("nav.workPermits")} />
            </div>

            <div className="nav-section nav-section--community">
              <p className="nav-group">{t("nav.groups.community")}</p>
              {isVecino ? (
                <>
                  <NavItem
                    to={`/communities/${communityId}/incidents`}
                    icon={<AlertTriangle size={18} />}
                    label={t("nav.incidents")}
                    accent="rose"
                  />
                  <NavItem
                    to={`/communities/${communityId}/reservations`}
                    icon={<CalendarCheck size={18} />}
                    label={t("nav.reservations")}
                    accent="violet"
                  />
                </>
              ) : null}
              <NavItem
                to={`/communities/${communityId}/chat`}
                icon={<MessageCircle size={18} />}
                label={t("nav.chat")}
                accent={isVecino ? "blue" : undefined}
              />
              <NavItem
                to={`/communities/${communityId}/board`}
                icon={<StickyNote size={18} />}
                label={t("nav.board")}
                accent={isVecino ? "amber" : undefined}
              />
              <NavItem
                to={`/communities/${communityId}/lost-found`}
                icon={<PackageSearch size={18} />}
                label={t("nav.lostFound")}
                accent={isVecino ? "teal" : undefined}
              />
              {canViewProviders(roles) ? (
                <NavItem
                  to={`/communities/${communityId}/providers`}
                  icon={<Wrench size={18} />}
                  label={t("nav.providers")}
                  accent={isVecino ? "orange" : undefined}
                />
              ) : null}
              <NavItem
                to={`/communities/${communityId}/votings`}
                icon={<Vote size={18} />}
                label={t("nav.votings")}
                accent={isVecino ? "indigo" : undefined}
              />
              <NavItem
                to={`/communities/${communityId}/members`}
                icon={<Users size={18} />}
                label={t("nav.members")}
                accent={isVecino ? "slate" : undefined}
              />
              <NavItem
                to={`/communities/${communityId}/authorizations`}
                icon={<UserCheck size={18} />}
                label={t("nav.authorizations")}
                accent={isVecino ? "cyan" : undefined}
              />
              <NavItem
                to={`/communities/${communityId}/directory`}
                icon={<BookUser size={18} />}
                label={t("nav.directory")}
                accent={isVecino ? "green" : undefined}
              />
            </div>
          </nav>
          {user ? (
            <div className="sidebar__footer">
              <div className="sidebar__user">
                <span className="avatar" aria-hidden="true">{initials(user.fullName)}</span>
                <div>
                  <strong>{user.fullName}</strong>
                  <small>{displayRoleLabel}</small>
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
          {delegationOptions.length ? (
            <div className="acting-bar" role="status">
              <UserCheck size={16} aria-hidden="true" />
              <label htmlFor="acting-as">{t("authorizations.actingAs")}</label>
              <select
                id="acting-as"
                value={activeDelegationKey}
                onChange={(event) => {
                  const value = event.target.value;
                  if (!value) {
                    clearDelegation();
                    return;
                  }
                  const authorization = delegationOptions.find((item) =>
                    `${item.scope}:${item.grantorMembershipId}` === value);
                  if (authorization) {
                    setActiveDelegation({
                      communityId: communityId!,
                      membershipId: authorization.grantorMembershipId,
                      scope: authorization.scope,
                      name: authorization.grantorName ?? "",
                    });
                  }
                }}
              >
                <option value="">{t("authorizations.asSelf")}</option>
                {delegationOptions.map((authorization) => (
                  <option
                    key={`${authorization.scope}:${authorization.grantorMembershipId}`}
                    value={`${authorization.scope}:${authorization.grantorMembershipId}`}
                  >
                    {authorization.grantorName ?? "—"} · {t(`authorizations.scopes.${authorization.scope}`)}
                  </option>
                ))}
              </select>
              {activeDelegation ? (
                <button type="button" className="text-link text-link--button" onClick={clearDelegation}>
                  {t("authorizations.stopActing")}
                </button>
              ) : (
                <span className="acting-bar__hint">{t("authorizations.chooseHint")}</span>
              )}
            </div>
          ) : null}
          {readOnly ? (
            <div className="readonly-banner" role="status">
              <Eye size={16} aria-hidden="true" />{" "}
              {activeDelegation
                ? t("common.delegatedReadOnly", { name: activeDelegation.name })
                : t("common.readOnly")}
            </div>
          ) : null}
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
            <div className="mobile-more__identity">
              <span className="avatar avatar--sm" aria-hidden="true">{initials(user?.fullName)}</span>
              <div>
                <strong>{user?.fullName}</strong>
                <span className={`role-chip ${isVecino ? "role-chip--vecino" : ""}`}>{displayRoleLabel}</span>
              </div>
            </div>
            <div className="mobile-more__header">
              <strong>{t("nav.more")}</strong>
              <button type="button" className="text-link text-link--button" onClick={() => setMoreOpen(false)}>
                {t("common.cancel")}
              </button>
            </div>
            <div className="mobile-more__grid">
              {orderedMobileLinks.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  className={isVecino && link.community ? `nav-link--accent-${link.accent}` : undefined}
                >
                  {link.label}
                </NavLink>
              ))}
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

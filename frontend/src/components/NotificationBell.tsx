import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
  useUnreadNotificationCount,
} from "../features/notifications/queries";
import type { AppNotification } from "../types/api";

function notificationTitle(t: (key: string, options?: Record<string, unknown>) => string,
                           notification: AppNotification): string {
  const title = String(notification.params.title ?? "");
  return t(`notifications.types.${notification.type}`, { title });
}

export function NotificationBell() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { communityId } = useParams<{ communityId: string }>();
  const [open, setOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const unread = useUnreadNotificationCount();
  const notifications = useNotifications();
  const markRead = useMarkNotificationRead();
  const markAll = useMarkAllNotificationsRead();
  const count = unread.data?.count ?? 0;

  useEffect(() => {
    if (!open) {
      return;
    }
    function handleClick(event: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [open]);

  async function openNotification(notification: AppNotification) {
    if (notification.status !== "READ") {
      await markRead.mutateAsync(notification.id);
    }
    const meetingId = notification.params.meetingId;
    if (typeof meetingId === "string" && communityId) {
      setOpen(false);
      navigate(`/communities/${communityId}/meetings/${meetingId}`);
    }
  }

  return (
    <div className="notification-bell" ref={panelRef}>
      <button
        type="button"
        className="notification-bell__button"
        aria-label={t("notifications.title")}
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
          <path d="M10 21h4" />
        </svg>
        {count > 0 ? <span className="notification-bell__badge">{count > 99 ? "99+" : count}</span> : null}
      </button>

      {open ? (
        <div className="notification-panel">
          <div className="notification-panel__header">
            <strong>{t("notifications.title")}</strong>
            {count > 0 ? (
              <button
                type="button"
                className="text-link text-link--button"
                onClick={() => void markAll.mutateAsync()}
              >
                {t("notifications.markAllRead")}
              </button>
            ) : null}
          </div>
          {notifications.data?.length ? (
            <ul className="notification-list">
              {notifications.data.slice(0, 8).map((notification) => (
                <li key={notification.id}>
                  <button
                    type="button"
                    className={`notification-item ${notification.status === "READ" ? "" : "notification-item--unread"}`}
                    onClick={() => void openNotification(notification)}
                  >
                    <span>{notificationTitle(t, notification)}</span>
                    <small>
                      {new Intl.DateTimeFormat(undefined, {
                        dateStyle: "short",
                        timeStyle: "short",
                      }).format(new Date(notification.createdAt))}
                    </small>
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="notification-panel__empty">{t("notifications.empty")}</p>
          )}
          {communityId ? (
            <Link
              className="notification-panel__footer"
              to={`/communities/${communityId}/notification-preferences`}
              onClick={() => setOpen(false)}
            >
              {t("notifications.preferences")}
            </Link>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

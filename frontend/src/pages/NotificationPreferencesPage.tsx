import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import {
  useNotificationPreferences,
  useUpdateNotificationPreferences,
} from "../features/notifications/queries";
import type { NotificationChannel, NotificationPreference } from "../types/api";
import { disablePush, enablePush, getPushState, type PushState } from "../services/push";

const CATEGORIES = ["MEETINGS", "INCIDENTS", "FINANCE", "SYSTEM"];
const CHANNELS: NotificationChannel[] = ["IN_APP", "EMAIL", "PUSH"];

export function NotificationPreferencesPage() {
  const { t } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const preferences = useNotificationPreferences();
  const update = useUpdateNotificationPreferences();
  const [values, setValues] = useState<NotificationPreference[]>([]);
  const [pushState, setPushState] = useState<PushState>("unavailable");
  const [pushBusy, setPushBusy] = useState(false);
  const [pushError, setPushError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    getPushState()
      .then((state) => {
        if (active) {
          setPushState(state);
        }
      })
      .catch(() => {
        if (active) {
          setPushState("unavailable");
        }
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (preferences.data) {
      setValues(preferences.data);
    }
  }, [preferences.data]);

  if (preferences.isLoading) {
    return <FullPageLoader />;
  }

  if (preferences.isError) {
    return (
      <div className="page">
        <Alert variant="error">{t("notifications.error")}</Alert>
      </div>
    );
  }

  function isEnabled(category: string, channel: NotificationChannel): boolean {
    return values.find((item) => item.category === category && item.channel === channel)?.enabled ?? true;
  }

  function toggle(category: string, channel: NotificationChannel) {
    setValues((current) => {
      const existing = current.find((item) => item.category === category && item.channel === channel);
      if (existing) {
        return current.map((item) =>
          item.category === category && item.channel === channel
            ? { ...item, enabled: !item.enabled }
            : item,
        );
      }
      return [...current, { category, channel, enabled: false }];
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await update.mutateAsync(
        CATEGORIES.flatMap((category) =>
          CHANNELS.map((channel) => ({ category, channel, enabled: isEnabled(category, channel) })),
        ),
      );
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function handleTogglePush() {
    setPushError(null);
    setPushBusy(true);
    try {
      if (pushState === "subscribed") {
        await disablePush();
        setPushState("unsubscribed");
      } else {
        await enablePush();
        setPushState("subscribed");
      }
    } catch (error) {
      setPushError(
        error instanceof Error && error.message === "PUSH_DENIED"
          ? t("notifications.push.denied")
          : t("notifications.push.error"),
      );
      setPushState(await getPushState().catch((): PushState => "unavailable"));
    } finally {
      setPushBusy(false);
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <Link className="breadcrumb" to={`/communities/${communityId}/dashboard`}>
            {t("common.back")}
          </Link>
          <h1>{t("notifications.preferences")}</h1>
          <p className="page-subtitle">{t("notifications.preferencesSubtitle")}</p>
        </div>
      </header>

      {update.isError ? <Alert variant="error">{t("notifications.error")}</Alert> : null}
      {update.isSuccess ? <Alert variant="success">{t("notifications.saved")}</Alert> : null}

      <form className="form-card" onSubmit={handleSubmit}>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("notifications.category")}</th>
                {CHANNELS.map((channel) => (
                  <th key={channel}>{t(`notifications.channels.${channel}`)}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {CATEGORIES.map((category) => (
                <tr key={category}>
                  <td data-label={t("notifications.category")}>{t(`notifications.categories.${category}`)}</td>
                  {CHANNELS.map((channel) => (
                    <td key={channel} data-label={t(`notifications.channels.${channel}`)}>
                      <label className="inline-check">
                        <input
                          type="checkbox"
                          checked={isEnabled(category, channel)}
                          onChange={() => toggle(category, channel)}
                        />
                        <span>{t("notifications.enabled")}</span>
                      </label>
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="form-actions">
          <Button type="submit" loading={update.isPending}>{t("common.save")}</Button>
        </div>
      </form>

      <section className="form-card">
        <h2>{t("notifications.push.title")}</h2>
        <p className="page-subtitle">{t("notifications.push.description")}</p>
        {pushError ? <Alert variant="error">{pushError}</Alert> : null}
        {pushState === "unsupported" ? (
          <Alert variant="info">{t("notifications.push.unsupported")}</Alert>
        ) : null}
        {pushState === "unavailable" ? (
          <Alert variant="info">{t("notifications.push.unavailable")}</Alert>
        ) : null}
        {pushState === "denied" ? (
          <Alert variant="error">{t("notifications.push.denied")}</Alert>
        ) : null}
        {pushState === "subscribed" || pushState === "unsubscribed" ? (
          <div className="form-actions">
            <Button type="button" onClick={handleTogglePush} loading={pushBusy}>
              {pushState === "subscribed"
                ? t("notifications.push.disable")
                : t("notifications.push.enable")}
            </Button>
          </div>
        ) : null}
      </section>
    </div>
  );
}

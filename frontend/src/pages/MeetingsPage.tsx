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
import { useCommunityStore } from "../features/communities/communityStore";
import { canManageMeetings } from "../features/meetings/utils";
import { useCreateMeeting, useMeetings } from "../features/meetings/queries";
import type { MeetingStatus, MeetingType } from "../types/api";
import { formatDateTime } from "../utils/format";

export function MeetingsPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const membership = user?.memberships.find((item) => item.id === communityId);
  const canManage = canManageMeetings(membership?.roles ?? []);
  const language = i18n.language.startsWith("ca") ? "ca" : "es";
  const [showForm, setShowForm] = useState(false);
  const meetings = useMeetings(communityId);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  if (meetings.isLoading) {
    return <FullPageLoader />;
  }

  if (meetings.isError) {
    return (
      <div className="page">
        <Alert variant="error">{t("meetings.error")}</Alert>
      </div>
    );
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{t("meetings.title")}</h1>
          <p className="page-subtitle">{t("meetings.subtitle")}</p>
        </div>
        {canManage ? (
          <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>
            {t("meetings.newMeeting")}
          </Button>
        ) : null}
      </header>

      {showForm && communityId ? (
        <MeetingForm communityId={communityId} onDone={() => setShowForm(false)} />
      ) : null}

      {meetings.data?.length === 0 ? (
        <div className="empty-state">{t("meetings.noMeetings")}</div>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("meetings.meetingTitle")}</th>
                <th>{t("meetings.type")}</th>
                <th>{t("meetings.date")}</th>
                <th>{t("meetings.location")}</th>
                <th>{t("meetings.status")}</th>
              </tr>
            </thead>
            <tbody>
              {meetings.data?.map((meeting) => (
                <tr key={meeting.id}>
                  <td data-label={t("meetings.meetingTitle")}>
                    <Link to={`/communities/${communityId}/meetings/${meeting.id}`}>
                      {meeting.title}
                    </Link>
                  </td>
                  <td data-label={t("meetings.type")}>{t(`meetings.types.${meeting.type}`)}</td>
                  <td data-label={t("meetings.date")}>{formatDateTime(meeting.scheduledStart, language)}</td>
                  <td data-label={t("meetings.location")}>{meeting.location ?? "—"}</td>
                  <td data-label={t("meetings.status")}>
                    <span className="badge">{t(`meetings.statuses.${meeting.status}`)}</span>
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

function MeetingForm({ communityId, onDone }: { communityId: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useCreateMeeting(communityId);
  const [title, setTitle] = useState("");
  const [type, setType] = useState<MeetingType>("ORDINARY");
  const [date, setDate] = useState("");
  const [location, setLocation] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!date) return;
    try {
      await mutation.mutateAsync({
        title,
        type,
        scheduledStart: new Date(date).toISOString(),
        location: location || undefined,
        status: "DRAFT" as MeetingStatus,
      });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("meetings.newMeeting")}</h3>
      {mutation.isError ? <Alert variant="error">{t("meetings.error")}</Alert> : null}
      <div className="form-grid">
        <Input label={t("meetings.meetingTitle")} value={title} onChange={(event) => setTitle(event.target.value)} required />
        <Select label={t("meetings.type")} value={type} onChange={(event) => setType(event.target.value as MeetingType)}>
          <option value="ORDINARY">{t("meetings.types.ORDINARY")}</option>
          <option value="EXTRAORDINARY">{t("meetings.types.EXTRAORDINARY")}</option>
        </Select>
        <Input
          label={t("meetings.date")}
          type="datetime-local"
          value={date}
          onChange={(event) => setDate(event.target.value)}
          required
        />
        <Input label={t("meetings.location")} value={location} onChange={(event) => setLocation(event.target.value)} />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("meetings.create")}</Button>
      </div>
    </form>
  );
}

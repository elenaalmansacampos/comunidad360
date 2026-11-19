import { useRef, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Select } from "../components/Select";
import { Textarea } from "../components/Textarea";
import { useAuthStore } from "../features/auth/authStore";
import { useMembers } from "../features/communities/queries";
import {
  useAddIncidentComment,
  useIncident,
  useUpdateIncident,
  useUploadIncidentAttachment,
} from "../features/incidents/queries";
import { canManageIncidents, nextIncidentStatuses } from "../features/incidents/utils";
import { downloadFile } from "../services/apiClient";
import type { IncidentStatus } from "../types/api";
import { formatDate } from "../utils/format";

export function IncidentDetailPage() {
  const { t, i18n } = useTranslation();
  const { communityId, incidentId } = useParams<{ communityId: string; incidentId: string }>();
  const user = useAuthStore((state) => state.user);
  const membership = user?.memberships.find((item) => item.id === communityId);
  const canManage = canManageIncidents(membership?.roles ?? []);
  const language = i18n.language.startsWith("ca") ? "ca" : "es";
  const incident = useIncident(communityId, incidentId);
  const members = useMembers(communityId, canManage);
  const updateIncident = useUpdateIncident(communityId, incidentId);
  const addComment = useAddIncidentComment(communityId, incidentId);
  const uploadAttachment = useUploadIncidentAttachment(communityId, incidentId);
  const [status, setStatus] = useState<IncidentStatus | "">("");
  const [assignedTo, setAssignedTo] = useState("");
  const [comment, setComment] = useState("");
  const [internal, setInternal] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  if (incident.isLoading) {
    return <FullPageLoader />;
  }

  if (incident.isError || !incident.data) {
    return (
      <div className="page">
        <Alert variant="error">{t("incidents.error")}</Alert>
      </div>
    );
  }

  const detail = incident.data;
  const data = detail.incident;
  const allowedStatuses = nextIncidentStatuses(data.status);
  const activeMembers = (members.data ?? []).filter((member) => member.status === "ACTIVE");

  async function updateStatus(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!status) return;
    try {
      await updateIncident.mutateAsync({ status, version: data.version });
      setStatus("");
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function updateAssignment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!assignedTo) return;
    try {
      await updateIncident.mutateAsync({ assignedToUserId: assignedTo, version: data.version });
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function addCommentSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await addComment.mutateAsync({ body: comment, internal });
      setComment("");
      setInternal(false);
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function uploadPhoto(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const file = fileRef.current?.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append("file", file);
    try {
      await uploadAttachment.mutateAsync(formData);
      if (fileRef.current) {
        fileRef.current.value = "";
      }
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function download(downloadUrl: string, fileName: string) {
    setDownloadError(null);
    try {
      const blob = await downloadFile(downloadUrl);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      setDownloadError(t("incidents.error"));
    }
  }

  return (
    <div className="page">
      <Link className="text-link" to={`/communities/${communityId}/incidents`}>
        ← {t("incidents.title")}
      </Link>
      <header className="page-header">
        <div>
          <h1>{data.title}</h1>
          <div className="chips">
            <span className="badge">{t(`incidents.statuses.${data.status}`)}</span>
            <span className="badge">{t(`incidents.priorities.${data.priority}`)}</span>
            <span className="badge badge--muted">{data.category}</span>
          </div>
        </div>
      </header>

      <section className="detail-meta">
        <div>
          <span>{t("incidents.reportedBy")}</span>
          <strong>{data.reportedByName ?? "—"}</strong>
        </div>
        <div>
          <span>{t("incidents.assignedTo")}</span>
          <strong>{data.assignedToName ?? t("incidents.unassigned")}</strong>
        </div>
        <div>
          <span>{t("incidents.unit")}</span>
          <strong>{data.unitCode ?? t("incidents.noUnit")}</strong>
        </div>
        <div>
          <span>{t("incidents.created")}</span>
          <strong>{formatDate(data.createdAt, language)}</strong>
        </div>
      </section>

      <section className="structure-section">
        <h2>{t("incidents.description")}</h2>
        <p>{data.description}</p>
      </section>

      {canManage ? (
        <section className="structure-section">
          <h2>{t("incidents.statusTitle")}</h2>
          {updateIncident.isError ? <Alert variant="error">{t("incidents.invalidTransition")}</Alert> : null}
          <div className="form-grid">
            {allowedStatuses.length > 0 ? (
              <form className="form-card" onSubmit={updateStatus}>
                <Select
                  label={t("incidents.status")}
                  value={status}
                  onChange={(event) => setStatus(event.target.value as IncidentStatus)}
                  required
                >
                  <option value="">—</option>
                  {allowedStatuses.map((value) => (
                    <option value={value} key={value}>{t(`incidents.statuses.${value}`)}</option>
                  ))}
                </Select>
                <div className="form-actions">
                  <Button type="submit" loading={updateIncident.isPending}>{t("incidents.updateStatus")}</Button>
                </div>
              </form>
            ) : null}
            <form className="form-card" onSubmit={updateAssignment}>
              <Select
                label={t("incidents.assignedTo")}
                value={assignedTo}
                onChange={(event) => setAssignedTo(event.target.value)}
                required
              >
                <option value="">—</option>
                {activeMembers.map((member) => (
                  <option value={member.userId} key={member.userId}>
                    {member.fullName} · {member.email}
                  </option>
                ))}
              </Select>
              <div className="form-actions">
                <Button type="submit" loading={updateIncident.isPending}>{t("incidents.assign")}</Button>
              </div>
            </form>
          </div>
        </section>
      ) : null}

      <section className="structure-section">
        <h2>{t("incidents.comments")}</h2>
        {detail.comments.length === 0 ? (
          <div className="empty-state">{t("incidents.noComments")}</div>
        ) : (
          <div className="comment-list">
            {detail.comments.map((item) => (
              <article className={`comment ${item.internal ? "comment--internal" : ""}`} key={item.id}>
                <div className="comment__header">
                  <strong>{item.authorName ?? "—"}</strong>
                  <span className="muted">{formatDate(item.createdAt, language)}</span>
                  {item.internal ? <span className="badge">{t("incidents.internalNote")}</span> : null}
                </div>
                <p>{item.body}</p>
              </article>
            ))}
          </div>
        )}
        <form className="form-card" onSubmit={addCommentSubmit}>
          {addComment.isError ? <Alert variant="error">{t("incidents.error")}</Alert> : null}
          <Textarea
            label={t("incidents.addComment")}
            placeholder={t("incidents.commentPlaceholder")}
            value={comment}
            onChange={(event) => setComment(event.target.value)}
            required
          />
          {canManage ? (
            <label className="inline-check">
              <input type="checkbox" checked={internal} onChange={(event) => setInternal(event.target.checked)} />
              <span>{t("incidents.internalNote")}</span>
            </label>
          ) : null}
          <div className="form-actions">
            <Button type="submit" loading={addComment.isPending}>{t("incidents.addComment")}</Button>
          </div>
        </form>
      </section>

      <section className="structure-section">
        <h2>{t("incidents.attachments")}</h2>
        {downloadError ? <Alert variant="error">{downloadError}</Alert> : null}
        {detail.attachments.length === 0 ? (
          <div className="empty-state">{t("incidents.noAttachments")}</div>
        ) : (
          <div className="attachment-list">
            {detail.attachments.map((attachment) => (
              <article className="list-card" key={attachment.id}>
                <h3>{attachment.fileName}</h3>
                <p className="muted">{attachment.mimeType} · {Math.round(attachment.sizeBytes / 1024)} KB</p>
                <Button
                  variant="ghost"
                  className="btn--small"
                  onClick={() => void download(attachment.downloadUrl, attachment.fileName)}
                >
                  {t("incidents.download")}
                </Button>
              </article>
            ))}
          </div>
        )}
        <form className="form-card" onSubmit={uploadPhoto}>
          {uploadAttachment.isError ? <Alert variant="error">{t("incidents.error")}</Alert> : null}
          <div className="field">
            <label className="field__label" htmlFor="incident-photo">{t("incidents.uploadPhoto")}</label>
            <input
              ref={fileRef}
              id="incident-photo"
              className="field__input"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              required
            />
            <p className="muted">{t("incidents.photoHint")}</p>
          </div>
          <div className="form-actions">
            <Button type="submit" loading={uploadAttachment.isPending}>{t("incidents.uploadPhoto")}</Button>
          </div>
        </form>
      </section>

      <section className="structure-section">
        <h2>{t("incidents.history")}</h2>
        {detail.history.length === 0 ? (
          <div className="empty-state">{t("incidents.noHistory")}</div>
        ) : (
          <ul className="timeline">
            {detail.history.map((entry) => (
              <li key={entry.id}>
                <strong>
                  {entry.fromStatus
                    ? `${t(`incidents.statuses.${entry.fromStatus}`)} → ${t(`incidents.statuses.${entry.toStatus}`)}`
                    : t(`incidents.statuses.${entry.toStatus}`)}
                </strong>
                <span className="muted">
                  {entry.changedByName ?? "—"} · {formatDate(entry.changedAt, language)}
                </span>
                {entry.note ? <p>{entry.note}</p> : null}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

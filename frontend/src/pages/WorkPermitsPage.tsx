import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Check, Download, HardHat, Paperclip, Plus, Trash2, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Textarea } from "../components/Textarea";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunityStore } from "../features/communities/communityStore";
import { canCreateWorkPermits, canManageWorkPermits, isReadOnly } from "../features/mvp/utils";
import {
  useCreateWorkPermit,
  useDeleteWorkPermit,
  useUpdateWorkPermit,
  useUploadWorkPermitFile,
  useWorkPermits,
} from "../features/works/queries";
import { downloadFile } from "../services/apiClient";
import type { WorkPermit } from "../types/api";
import { formatDate } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

export function WorkPermitsPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const roles = user?.memberships.find((item) => item.id === communityId)?.roles ?? [];
  const readOnly = isReadOnly(roles);
  const canCreate = !readOnly && canCreateWorkPermits(roles);
  const canManage = canManageWorkPermits(roles);
  const language = normalizeLanguage(i18n.language);
  const permits = useWorkPermits(communityId);
  const create = useCreateWorkPermit(communityId);
  const update = useUpdateWorkPermit(communityId);
  const upload = useUploadWorkPermitFile(communityId);
  const remove = useDeleteWorkPermit(communityId);
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [file, setFile] = useState<File | null>(null);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const created = await create.mutateAsync({
        title,
        description: description || undefined,
        location: location || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      });
      if (file) {
        await upload.mutateAsync({ permitId: created.id, file });
      }
      setTitle("");
      setDescription("");
      setLocation("");
      setStartDate("");
      setEndDate("");
      setFile(null);
      setShowForm(false);
    } catch {
      // estado de la mutación
    }
  }

  async function download(permit: WorkPermit) {
    if (!permit.fileUrl) {
      return;
    }
    const blob = await downloadFile(permit.fileUrl);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = permit.fileName ?? "permiso";
    link.click();
    URL.revokeObjectURL(url);
  }

  async function setStatus(permit: WorkPermit, status: WorkPermit["status"]) {
    try {
      await update.mutateAsync({ permitId: permit.id, status, version: permit.version });
    } catch {
      // estado de la mutación
    }
  }

  function canEdit(permit: WorkPermit): boolean {
    return !readOnly && (canManage || permit.createdByUserId === user?.id);
  }

  const failed = create.isError || update.isError || upload.isError || remove.isError;

  if (permits.isLoading) {
    return <FullPageLoader />;
  }

  return (
    <div className="page permits-page">
      <header className="page-header">
        <div>
          <h1>{t("workPermits.title")}</h1>
          <p className="page-subtitle">{t("workPermits.subtitle")}</p>
        </div>
        {canCreate ? (
          <Button onClick={() => setShowForm((value) => !value)}>
            <Plus size={16} aria-hidden="true" /> {t("workPermits.newPermit")}
          </Button>
        ) : null}
      </header>

      {permits.isError ? <Alert variant="error">{t("workPermits.error")}</Alert> : null}
      {failed ? <Alert variant="error">{t("workPermits.error")}</Alert> : null}

      {showForm && canCreate ? (
        <form className="form-card" onSubmit={submit}>
          <h3>{t("workPermits.newPermit")}</h3>
          <div className="form-grid">
            <Input label={t("workPermits.permitTitle")} value={title} onChange={(event) => setTitle(event.target.value)} required />
            <Input label={t("workPermits.location")} value={location} onChange={(event) => setLocation(event.target.value)} />
            <Input label={t("workPermits.startDate")} type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
            <Input label={t("workPermits.endDate")} type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
            <div className="field">
              <label className="field__label" htmlFor="permit-file">{t("workPermits.file")}</label>
              <input
                id="permit-file"
                className="field__input"
                type="file"
                accept="application/pdf,image/jpeg,image/png,image/webp"
                onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              />
              <p className="field__hint">{t("workPermits.fileHint")}</p>
            </div>
          </div>
          <Textarea label={t("workPermits.description")} value={description} onChange={(event) => setDescription(event.target.value)} rows={3} />
          <div className="form-actions">
            <Button type="submit" loading={create.isPending || upload.isPending}>{t("common.save")}</Button>
            <Button type="button" variant="secondary" onClick={() => setShowForm(false)}>{t("common.cancel")}</Button>
          </div>
        </form>
      ) : null}

      {permits.data?.length ? (
        <div className="permits-grid">
          {permits.data.map((permit) => {
            const editable = canEdit(permit);
            return (
              <article className={`permit-card permit-card--${permit.status.toLowerCase()}`} key={permit.id}>
                <header className="permit-card__head">
                  <span className={`permit-status permit-status--${permit.status.toLowerCase()}`}>
                    {t(`workPermits.statuses.${permit.status}`)}
                  </span>
                  {permit.fileUrl ? (
                    <Button variant="ghost" className="btn--small" onClick={() => void download(permit)}>
                      <Download size={14} aria-hidden="true" /> {permit.fileName ?? t("workPermits.download")}
                    </Button>
                  ) : null}
                </header>
                <h2>{permit.title}</h2>
                {permit.description ? <p>{permit.description}</p> : null}
                <dl className="permit-card__meta">
                  {permit.location ? (
                    <div><dt>{t("workPermits.location")}</dt><dd>{permit.location}</dd></div>
                  ) : null}
                  <div><dt>{t("workPermits.startDate")}</dt><dd>{permit.startDate ? formatDate(new Date(`${permit.startDate}T00:00:00`), language) : "—"}</dd></div>
                  <div><dt>{t("workPermits.endDate")}</dt><dd>{permit.endDate ? formatDate(new Date(`${permit.endDate}T00:00:00`), language) : "—"}</dd></div>
                </dl>
                {permit.reviewedByName ? (
                  <p className="permit-card__review">
                    {t("workPermits.reviewedBy", { name: permit.reviewedByName })}
                    {permit.reviewNotes ? `: ${permit.reviewNotes}` : ""}
                  </p>
                ) : null}
                <footer className="permit-card__foot">
                  <small>{permit.createdByName ?? "—"}</small>
                  {editable ? (
                    <div className="permit-card__actions">
                      {!permit.fileUrl ? (
                        <label className="btn btn--ghost btn--small permit-file-upload">
                          <Paperclip size={14} aria-hidden="true" /> {t("workPermits.attachFile")}
                          <input
                            type="file"
                            accept="application/pdf,image/jpeg,image/png,image/webp"
                            hidden
                            onChange={(event) => {
                              const input = event.currentTarget;
                              const selected = input.files?.[0];
                              if (selected) {
                                void upload.mutateAsync({ permitId: permit.id, file: selected });
                              }
                              input.value = "";
                            }}
                          />
                        </label>
                      ) : null}
                      {canManage && permit.status === "REQUESTED" ? (
                        <>
                          <Button variant="secondary" className="btn--small" onClick={() => void setStatus(permit, "APPROVED")}>
                            <Check size={14} aria-hidden="true" /> {t("workPermits.approve")}
                          </Button>
                          <Button variant="ghost" className="btn--small" onClick={() => void setStatus(permit, "REJECTED")}>
                            <X size={14} aria-hidden="true" /> {t("workPermits.reject")}
                          </Button>
                        </>
                      ) : null}
                      {permit.status === "REQUESTED" && permit.createdByUserId === user?.id ? (
                        <Button variant="ghost" className="btn--small" onClick={() => void setStatus(permit, "CANCELLED")}>
                          {t("workPermits.cancel")}
                        </Button>
                      ) : null}
                      <Button variant="ghost" className="btn--small" onClick={() => void remove.mutateAsync(permit.id)}>
                        <Trash2 size={14} aria-hidden="true" /> {t("workPermits.delete")}
                      </Button>
                    </div>
                  ) : null}
                </footer>
              </article>
            );
          })}
        </div>
      ) : (
        <div className="empty-state lost-empty">
          <HardHat size={28} aria-hidden="true" />
          <span>{t("workPermits.empty")}</span>
        </div>
      )}
    </div>
  );
}

import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import {
  ChevronDown,
  ChevronUp,
  ImagePlus,
  MessageSquareText,
  PackageSearch,
  Paperclip,
  Plus,
  Trash2,
  Upload,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { Textarea } from "../components/Textarea";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunityStore } from "../features/communities/communityStore";
import { downloadDocument, formatFileSize } from "../features/documents/utils";
import {
  useAddLostItemNote,
  useCreateLostItem,
  useDeleteLostItem,
  useDeleteLostItemAttachment,
  useDeleteLostItemNote,
  useLostItems,
  useUpdateLostItem,
  useUploadLostItemAttachment,
  useUploadLostItemPhoto,
} from "../features/lostfound/queries";
import { canManageLostFound, isReadOnly } from "../features/mvp/utils";
import { downloadFile } from "../services/apiClient";
import type { LostItem, LostItemAttachment, LostItemNote, LostItemStatus, LostItemType } from "../types/api";
import { formatDate, formatDateTime } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

type Filter = "ALL" | LostItemType;

function LostItemPhoto({ photoUrl }: { photoUrl?: string | null }) {
  const [url, setUrl] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    let objectUrl: string | null = null;
    setUrl(null);
    if (photoUrl) {
      downloadFile(photoUrl)
        .then((blob) => {
          objectUrl = URL.createObjectURL(blob);
          if (active) {
            setUrl(objectUrl);
          }
        })
        .catch(() => {
          if (active) {
            setUrl(null);
          }
        });
    }
    return () => {
      active = false;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [photoUrl]);

  if (url) {
    return <img className="lost-card__photo" src={url} alt="" />;
  }
  return (
    <div className="lost-card__photo lost-card__photo--empty" aria-hidden="true">
      <PackageSearch size={22} />
    </div>
  );
}

interface LostItemCardProps {
  item: LostItem;
  currentUserId?: string;
  readOnly: boolean;
  canManage: boolean;
  photoPending: boolean;
  notePending: boolean;
  attachmentPending: boolean;
  onSetStatus: (item: LostItem, status: LostItemStatus) => void;
  onDeleteItem: (item: LostItem) => void;
  onUploadPhoto: (item: LostItem, file: File) => void;
  onAddNote: (item: LostItem, body: string) => Promise<void>;
  onDeleteNote: (item: LostItem, note: LostItemNote) => void;
  onUploadAttachment: (item: LostItem, file: File) => Promise<void>;
  onDeleteAttachment: (item: LostItem, attachment: LostItemAttachment) => void;
}

function LostItemCard({
  item,
  currentUserId,
  readOnly,
  canManage,
  photoPending,
  notePending,
  attachmentPending,
  onSetStatus,
  onDeleteItem,
  onUploadPhoto,
  onAddNote,
  onDeleteNote,
  onUploadAttachment,
  onDeleteAttachment,
}: LostItemCardProps) {
  const { t, i18n } = useTranslation();
  const language = normalizeLanguage(i18n.language);
  const [open, setOpen] = useState(false);
  const [noteDraft, setNoteDraft] = useState("");
  const notes = item.notes ?? [];
  const attachments = item.attachments ?? [];
  const canEdit = !readOnly && (canManage || item.createdByUserId === currentUserId);
  const canDeleteNote = (note: LostItemNote) =>
    !readOnly && (canManage || note.authorUserId === currentUserId);
  const canDeleteAttachment = (attachment: LostItemAttachment) =>
    !readOnly && (canManage || attachment.uploadedByUserId === currentUserId);

  async function submitNote() {
    const body = noteDraft.trim();
    if (!body) {
      return;
    }
    try {
      await onAddNote(item, body);
      setNoteDraft("");
    } catch {
      // el error se muestra en el aviso global
    }
  }

  async function uploadAttachment(file: File) {
    try {
      await onUploadAttachment(item, file);
    } catch {
      // el error se muestra en el aviso global
    }
  }

  async function download(attachment: LostItemAttachment) {
    try {
      await downloadDocument(attachment.downloadUrl, attachment.fileName);
    } catch {
      // el error se muestra en el aviso global
    }
  }

  return (
    <article className={`lost-card lost-card--${item.type.toLowerCase()}`}>
      <LostItemPhoto photoUrl={item.photoUrl} />
      <header className="lost-card__head">
        <span className={`lost-card__type lost-card__type--${item.type.toLowerCase()}`}>
          {t(`lostFound.types.${item.type}`)}
        </span>
        <span className="badge badge--muted">{t(`lostFound.statuses.${item.status}`)}</span>
      </header>
      <h2>{item.title}</h2>
      {item.description ? <p>{item.description}</p> : null}
      <dl className="lost-card__meta">
        {item.location ? (
          <div><dt>{t("lostFound.location")}</dt><dd>{item.location}</dd></div>
        ) : null}
        {item.eventDate ? (
          <div><dt>{t("lostFound.eventDate")}</dt><dd>{formatDate(new Date(`${item.eventDate}T00:00:00`), language)}</dd></div>
        ) : null}
        {item.contact ? (
          <div><dt>{t("lostFound.contact")}</dt><dd>{item.contact}</dd></div>
        ) : null}
      </dl>

      <button
        type="button"
        className="lost-card__extras-toggle"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <span><MessageSquareText size={15} aria-hidden="true" /> {t("lostFound.notes")} ({notes.length})</span>
        <span><Paperclip size={15} aria-hidden="true" /> {t("lostFound.attachments")} ({attachments.length})</span>
        {open ? <ChevronUp size={16} aria-hidden="true" /> : <ChevronDown size={16} aria-hidden="true" />}
      </button>

      {open ? (
        <div className="lost-card__extras">
          <section className="lost-subsection">
            <h3 className="lost-subsection__title">
              <MessageSquareText size={15} aria-hidden="true" /> {t("lostFound.notes")}
            </h3>
            {notes.length ? (
              <ul className="lost-notes">
                {notes.map((note) => (
                  <li className="lost-note" key={note.id}>
                    <div className="lost-note__head">
                      <strong>{note.authorName ?? "—"}</strong>
                      <span>{formatDateTime(note.createdAt, language)}</span>
                      {canDeleteNote(note) ? (
                        <button
                          type="button"
                          className="lost-delete"
                          title={t("lostFound.deleteNote")}
                          aria-label={t("lostFound.deleteNote")}
                          onClick={() => onDeleteNote(item, note)}
                        >
                          <Trash2 size={13} aria-hidden="true" />
                        </button>
                      ) : null}
                    </div>
                    <p>{note.body}</p>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="muted">{t("lostFound.notesEmpty")}</p>
            )}
            {!readOnly ? (
              <div className="lost-note-form">
                <Textarea
                  label={t("lostFound.notePlaceholder")}
                  value={noteDraft}
                  onChange={(event) => setNoteDraft(event.target.value)}
                  rows={2}
                />
                <Button
                  type="button"
                  variant="secondary"
                  loading={notePending}
                  disabled={!noteDraft.trim()}
                  onClick={() => void submitNote()}
                >
                  {t("lostFound.addNote")}
                </Button>
              </div>
            ) : null}
          </section>

          <section className="lost-subsection">
            <h3 className="lost-subsection__title">
              <Paperclip size={15} aria-hidden="true" /> {t("lostFound.attachments")}
            </h3>
            {attachments.length ? (
              <ul className="lost-attachments">
                {attachments.map((attachment) => (
                  <li className="lost-attachment" key={attachment.id}>
                    <button type="button" className="lost-attachment__link" onClick={() => void download(attachment)}>
                      <Paperclip size={14} aria-hidden="true" />
                      <span>{attachment.fileName}</span>
                      <small>{formatFileSize(attachment.sizeBytes, language)}</small>
                    </button>
                    {canDeleteAttachment(attachment) ? (
                      <button
                        type="button"
                        className="lost-delete"
                        title={t("lostFound.deleteAttachment")}
                        aria-label={t("lostFound.deleteAttachment")}
                        onClick={() => onDeleteAttachment(item, attachment)}
                      >
                        <Trash2 size={13} aria-hidden="true" />
                      </button>
                    ) : null}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="muted">{t("lostFound.attachmentsEmpty")}</p>
            )}
            {!readOnly ? (
              <div className="lost-attachment-form">
                <label className="btn btn--ghost btn--small lost-attachment-upload" aria-busy={attachmentPending}>
                  <Upload size={14} aria-hidden="true" /> {t("lostFound.addAttachment")}
                  <input
                    type="file"
                    hidden
                    disabled={attachmentPending}
                    accept="image/jpeg,image/png,image/webp,application/pdf"
                    onChange={(event) => {
                      const input = event.currentTarget;
                      const file = input.files?.[0];
                      if (file) {
                        void uploadAttachment(file);
                      }
                      input.value = "";
                    }}
                  />
                </label>
                <p className="field__hint">{t("lostFound.attachmentHint")}</p>
              </div>
            ) : null}
          </section>
        </div>
      ) : null}

      <footer className="lost-card__foot">
        <small>{item.createdByName ?? "—"}</small>
        {canEdit ? (
          <div className="lost-card__actions">
            {item.status === "OPEN" ? (
              <>
                <Button variant="secondary" onClick={() => onSetStatus(item, "RESOLVED")}>
                  {t("lostFound.resolve")}
                </Button>
                <Button variant="ghost" onClick={() => onSetStatus(item, "ARCHIVED")}>
                  {t("lostFound.archive")}
                </Button>
              </>
            ) : (
              <Button variant="secondary" onClick={() => onSetStatus(item, "OPEN")}>
                {t("lostFound.reopen")}
              </Button>
            )}
            <label className="btn btn--ghost btn--small lost-photo-upload">
              <ImagePlus size={14} aria-hidden="true" /> {t("lostFound.changePhoto")}
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                hidden
                disabled={photoPending}
                onChange={(event) => {
                  const input = event.currentTarget;
                  const file = input.files?.[0];
                  if (file) {
                    onUploadPhoto(item, file);
                  }
                  input.value = "";
                }}
              />
            </label>
            <Button variant="ghost" onClick={() => onDeleteItem(item)}>
              {t("lostFound.delete")}
            </Button>
          </div>
        ) : null}
      </footer>
    </article>
  );
}

export function LostFoundPage() {
  const { t } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const roles = user?.memberships.find((item) => item.id === communityId)?.roles ?? [];
  const canManage = canManageLostFound(roles);
  const readOnly = isReadOnly(roles);
  const [filter, setFilter] = useState<Filter>("ALL");
  const items = useLostItems(communityId, filter === "ALL" ? undefined : filter);
  const create = useCreateLostItem(communityId);
  const update = useUpdateLostItem(communityId);
  const remove = useDeleteLostItem(communityId);
  const upload = useUploadLostItemPhoto(communityId);
  const addNote = useAddLostItemNote(communityId);
  const deleteNote = useDeleteLostItemNote(communityId);
  const addAttachment = useUploadLostItemAttachment(communityId);
  const deleteAttachment = useDeleteLostItemAttachment(communityId);
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState("");
  const [type, setType] = useState<LostItemType>("LOST");
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [eventDate, setEventDate] = useState("");
  const [contact, setContact] = useState("");
  const [photo, setPhoto] = useState<File | null>(null);

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
        type,
        location: location || undefined,
        eventDate: eventDate || undefined,
        contact: contact || undefined,
      });
      if (photo) {
        await upload.mutateAsync({ itemId: created.id, file: photo });
      }
      setTitle("");
      setDescription("");
      setLocation("");
      setEventDate("");
      setContact("");
      setPhoto(null);
      setShowForm(false);
    } catch {
      // estado de la mutación
    }
  }

  async function setStatus(item: LostItem, status: LostItemStatus) {
    try {
      await update.mutateAsync({ itemId: item.id, status, version: item.version });
    } catch {
      // estado de la mutación
    }
  }

  if (items.isLoading) {
    return <FullPageLoader />;
  }

  return (
    <div className="page lost-page">
      <header className="page-header">
        <div>
          <h1>{t("lostFound.title")}</h1>
          <p className="page-subtitle">{t("lostFound.subtitle")}</p>
        </div>
        {!readOnly ? (
          <Button onClick={() => setShowForm((value) => !value)}>
            <Plus size={16} aria-hidden="true" /> {t("lostFound.newItem")}
          </Button>
        ) : null}
      </header>

      {items.isError ? <Alert variant="error">{t("lostFound.error")}</Alert> : null}
      {create.isError || update.isError || remove.isError || upload.isError
        || addNote.isError || deleteNote.isError || addAttachment.isError || deleteAttachment.isError ? (
        <Alert variant="error">{t("lostFound.error")}</Alert>
      ) : null}

      <div className="tabs" role="tablist">
        {(["ALL", "LOST", "FOUND"] as Filter[]).map((value) => (
          <button
            type="button"
            key={value}
            role="tab"
            aria-selected={filter === value}
            className={`tab ${filter === value ? "tab--active" : ""}`}
            onClick={() => setFilter(value)}
          >
            {value === "ALL" ? t("lostFound.filters.all") : t(`lostFound.types.${value}`)}
          </button>
        ))}
      </div>

      {showForm && !readOnly ? (
        <form className="form-card" onSubmit={submit}>
          <h3>{t("lostFound.newItem")}</h3>
          <div className="form-grid">
            <Input label={t("lostFound.itemTitle")} value={title} onChange={(event) => setTitle(event.target.value)} required />
            <Select label={t("lostFound.type")} value={type} onChange={(event) => setType(event.target.value as LostItemType)}>
              <option value="LOST">{t("lostFound.types.LOST")}</option>
              <option value="FOUND">{t("lostFound.types.FOUND")}</option>
            </Select>
            <Input label={t("lostFound.location")} value={location} onChange={(event) => setLocation(event.target.value)} />
            <Input label={t("lostFound.eventDate")} type="date" value={eventDate} onChange={(event) => setEventDate(event.target.value)} />
            <Input label={t("lostFound.contact")} value={contact} onChange={(event) => setContact(event.target.value)} />
            <div className="field">
              <label className="field__label" htmlFor="lost-photo">{t("lostFound.photo")}</label>
              <input
                id="lost-photo"
                className="field__input"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={(event) => setPhoto(event.target.files?.[0] ?? null)}
              />
              <p className="field__hint">{t("lostFound.photoHint")}</p>
            </div>
          </div>
          <Textarea label={t("lostFound.description")} value={description} onChange={(event) => setDescription(event.target.value)} rows={3} />
          <div className="form-actions">
            <Button type="submit" loading={create.isPending || upload.isPending}>{t("common.save")}</Button>
            <Button type="button" variant="secondary" onClick={() => setShowForm(false)}>{t("common.cancel")}</Button>
          </div>
        </form>
      ) : null}

      {items.data?.length ? (
        <div className="lost-grid">
          {items.data.map((item) => (
            <LostItemCard
              key={item.id}
              item={item}
              currentUserId={user?.id}
              readOnly={readOnly}
              canManage={canManage}
              photoPending={upload.isPending && upload.variables?.itemId === item.id}
              notePending={addNote.isPending && addNote.variables?.itemId === item.id}
              attachmentPending={addAttachment.isPending && addAttachment.variables?.itemId === item.id}
              onSetStatus={(target, status) => { void setStatus(target, status); }}
              onDeleteItem={(target) => { void remove.mutateAsync(target.id); }}
              onUploadPhoto={(target, file) => { void upload.mutateAsync({ itemId: target.id, file }); }}
              onAddNote={(target, body) =>
                addNote.mutateAsync({ itemId: target.id, body }).then(() => undefined)}
              onDeleteNote={(target, note) => {
                void deleteNote.mutateAsync({ itemId: target.id, noteId: note.id });
              }}
              onUploadAttachment={(target, file) =>
                addAttachment.mutateAsync({ itemId: target.id, file }).then(() => undefined)}
              onDeleteAttachment={(target, attachment) => {
                void deleteAttachment.mutateAsync({ itemId: target.id, attachmentId: attachment.id });
              }}
            />
          ))}
        </div>
      ) : (
        <div className="empty-state lost-empty">
          <PackageSearch size={28} aria-hidden="true" />
          <span>{t("lostFound.empty")}</span>
        </div>
      )}
    </div>
  );
}

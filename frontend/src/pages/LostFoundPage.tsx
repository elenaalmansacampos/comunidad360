import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ImagePlus, PackageSearch, Plus } from "lucide-react";
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
import {
  useCreateLostItem,
  useDeleteLostItem,
  useLostItems,
  useUpdateLostItem,
  useUploadLostItemPhoto,
} from "../features/lostfound/queries";
import { downloadFile } from "../services/apiClient";
import { canManageLostFound, isReadOnly } from "../features/mvp/utils";
import type { LostItem, LostItemStatus, LostItemType } from "../types/api";
import { formatDate } from "../utils/format";
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

export function LostFoundPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const roles = user?.memberships.find((item) => item.id === communityId)?.roles ?? [];
  const canManage = canManageLostFound(roles);
  const readOnly = isReadOnly(roles);
  const language = normalizeLanguage(i18n.language);
  const [filter, setFilter] = useState<Filter>("ALL");
  const items = useLostItems(communityId, filter === "ALL" ? undefined : filter);
  const create = useCreateLostItem(communityId);
  const update = useUpdateLostItem(communityId);
  const remove = useDeleteLostItem(communityId);
  const upload = useUploadLostItemPhoto(communityId);
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

  function canEdit(item: LostItem): boolean {
    return !readOnly && (canManage || item.createdByUserId === user?.id);
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
      {create.isError || update.isError || remove.isError || upload.isError ? (
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
            <article className={`lost-card lost-card--${item.type.toLowerCase()}`} key={item.id}>
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
              <footer className="lost-card__foot">
                <small>{item.createdByName ?? "—"}</small>
                {canEdit(item) ? (
                  <div className="lost-card__actions">
                    {item.status === "OPEN" ? (
                      <>
                        <Button variant="secondary" onClick={() => void setStatus(item, "RESOLVED")}>
                          {t("lostFound.resolve")}
                        </Button>
                        <Button variant="ghost" onClick={() => void setStatus(item, "ARCHIVED")}>
                          {t("lostFound.archive")}
                        </Button>
                      </>
                    ) : (
                      <Button variant="secondary" onClick={() => void setStatus(item, "OPEN")}>
                        {t("lostFound.reopen")}
                      </Button>
                    )}
                    <label className="btn btn--ghost btn--small lost-photo-upload">
                      <ImagePlus size={14} aria-hidden="true" /> {t("lostFound.changePhoto")}
                      <input
                        type="file"
                        accept="image/jpeg,image/png,image/webp"
                        hidden
                        onChange={(event) => {
                          const input = event.currentTarget;
                          const file = input.files?.[0];
                          if (file) {
                            void upload.mutateAsync({ itemId: item.id, file });
                          }
                          input.value = "";
                        }}
                      />
                    </label>
                    <Button variant="ghost" onClick={() => void remove.mutateAsync(item.id)}>
                      {t("lostFound.delete")}
                    </Button>
                  </div>
                ) : null}
              </footer>
            </article>
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

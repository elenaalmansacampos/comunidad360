import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { Mail, Phone, Search, UserRound } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { useCommunityStore } from "../features/communities/communityStore";
import { useDirectory, useUpdateDirectoryProfile } from "../features/directory/queries";
import type { DirectoryEntry } from "../types/api";

function initials(name: string | null | undefined): string {
  if (!name) {
    return "?";
  }
  return name.split(" ").filter(Boolean).slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "").join("");
}

export function DirectoryPage() {
  const { t } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const directory = useDirectory(communityId);
  const update = useUpdateDirectoryProfile(communityId);
  const [search, setSearch] = useState("");
  const [phone, setPhone] = useState("");
  const [showEmail, setShowEmail] = useState(true);
  const [showPhone, setShowPhone] = useState(false);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  const self = directory.data?.find((entry) => entry.self);
  useEffect(() => {
    if (self) {
      setPhone(self.phone ?? "");
      setShowEmail(self.showEmail);
      setShowPhone(self.showPhone);
    }
  }, [self?.membershipId, self?.phone, self?.showEmail, self?.showPhone]);

  const entries = useMemo(() => {
    const term = search.trim().toLowerCase();
    const list = directory.data ?? [];
    if (!term) {
      return list;
    }
    return list.filter((entry) =>
      (entry.fullName ?? "").toLowerCase().includes(term)
      || entry.unitCodes.some((code) => code.toLowerCase().includes(term))
      || entry.roles.some((role) => t(`roles.${role}`, { defaultValue: role }).toLowerCase().includes(term)));
  }, [directory.data, search, t]);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await update.mutateAsync({ phone: phone || undefined, showEmail, showPhone });
    } catch {
      // error mostrado en el aviso
    }
  }

  if (directory.isLoading) {
    return <FullPageLoader />;
  }

  return (
    <div className="page directory-page">
      <header className="page-header">
        <div>
          <h1>{t("directory.title")}</h1>
          <p className="page-subtitle">{t("directory.subtitle")}</p>
        </div>
        <span className="badge badge--muted">{directory.data?.length ?? 0}</span>
      </header>

      {directory.isError ? <Alert variant="error">{t("directory.error")}</Alert> : null}
      {update.isError ? <Alert variant="error">{t("directory.error")}</Alert> : null}
      {update.isSuccess ? <Alert variant="success">{t("directory.saved")}</Alert> : null}

      {self ? (
        <form className="form-card directory-self" onSubmit={save}>
          <h3>{t("directory.myData")}</h3>
          <p className="page-subtitle">{t("directory.myDataHint")}</p>
          <div className="form-grid">
            <Input label={t("directory.phone")} type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} />
            <label className="inline-check">
              <input type="checkbox" checked={showEmail} onChange={(event) => setShowEmail(event.target.checked)} />
              <span>{t("directory.showEmail")}</span>
            </label>
            <label className="inline-check">
              <input type="checkbox" checked={showPhone} onChange={(event) => setShowPhone(event.target.checked)} />
              <span>{t("directory.showPhone")}</span>
            </label>
          </div>
          <div className="form-actions">
            <Button type="submit" loading={update.isPending}>{t("common.save")}</Button>
          </div>
        </form>
      ) : null}

      <div className="directory-search">
        <Input
          label={t("directory.search")}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder={t("directory.searchPlaceholder")}
        />
        <Search size={16} aria-hidden="true" />
      </div>

      {entries.length ? (
        <div className="directory-grid">
          {entries.map((entry) => (
            <DirectoryCard entry={entry} key={entry.membershipId} />
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <UserRound size={26} aria-hidden="true" />
          <span>{t("directory.noResults")}</span>
        </div>
      )}
    </div>
  );
}

function DirectoryCard({ entry }: { entry: DirectoryEntry }) {
  const { t } = useTranslation();
  return (
    <article className="directory-card">
      <header className="directory-card__head">
        <span className="avatar directory-card__avatar" aria-hidden="true">{initials(entry.fullName)}</span>
        <div>
          <h2>{entry.fullName ?? "—"}</h2>
          <div className="chips">
            {entry.roles.map((role) => (
              <span className="badge" key={role}>{t(`roles.${role}`, { defaultValue: role })}</span>
            ))}
            {entry.self ? <span className="badge badge--success">{t("directory.you")}</span> : null}
          </div>
        </div>
      </header>
      {entry.unitCodes.length ? (
        <p className="directory-card__units">{t("directory.units")}: {entry.unitCodes.join(", ")}</p>
      ) : null}
      <div className="directory-card__contact">
        {entry.email ? (
          <a href={`mailto:${entry.email}`}>
            <Mail size={15} aria-hidden="true" /> {entry.email}
          </a>
        ) : null}
        {entry.phone ? (
          <a href={`tel:${entry.phone}`}>
            <Phone size={15} aria-hidden="true" /> {entry.phone}
          </a>
        ) : null}
        {!entry.email && !entry.phone ? (
          <span className="muted">{t("directory.contactPrivate")}</span>
        ) : null}
      </div>
    </article>
  );
}

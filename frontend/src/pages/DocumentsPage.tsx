import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { Textarea } from "../components/Textarea";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunityStore } from "../features/communities/communityStore";
import { useCreateDocument, useDocuments } from "../features/documents/queries";
import { ApiError } from "../services/apiClient";
import { canUploadDocuments, downloadDocument, formatFileSize } from "../features/documents/utils";
import type { DocumentCategory, DocumentVisibility } from "../types/api";
import { formatDate } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

const CATEGORIES: DocumentCategory[] = [
  "ACTA",
  "ESTATUTO",
  "CONTRATO",
  "FACTURA",
  "PRESUPUESTO",
  "INFORME",
  "CERTIFICADO",
  "OTRO",
];

const VISIBILITIES: DocumentVisibility[] = ["COMMUNITY", "BOARD", "OWNERS", "PRIVATE"];

export function DocumentsPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const membership = user?.memberships.find((item) => item.id === communityId);
  const roles = membership?.roles ?? [];
  const canUpload = canUploadDocuments(roles);
  const language = normalizeLanguage(i18n.language);
  const [category, setCategory] = useState<DocumentCategory | "">("");
  const [showForm, setShowForm] = useState(false);
  const [downloadError, setDownloadError] = useState<"NONE" | "MISSING" | "GENERIC">("NONE");
  const documents = useDocuments(communityId, category || undefined);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  if (documents.isLoading) {
    return <FullPageLoader />;
  }

  if (documents.isError) {
    return (
      <div className="page">
        <Alert variant="error">{t("documents.error")}</Alert>
      </div>
    );
  }

  async function handleDownload(url: string, fileName: string) {
    setDownloadError("NONE");
    try {
      await downloadDocument(url, fileName);
    } catch (error) {
      setDownloadError(error instanceof ApiError && error.code === "FILE_NOT_FOUND" ? "MISSING" : "GENERIC");
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{t("documents.title")}</h1>
          <p className="page-subtitle">{t("documents.subtitle")}</p>
        </div>
        {canUpload ? (
          <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>
            {t("documents.newDocument")}
          </Button>
        ) : null}
      </header>

      {showForm && communityId ? (
        <DocumentForm communityId={communityId} onDone={() => setShowForm(false)} />
      ) : null}

      {downloadError !== "NONE" ? (
        <Alert variant="error">
          {downloadError === "MISSING" ? t("documents.fileMissing") : t("documents.downloadError")}
        </Alert>
      ) : null}

      <div className="filter-bar">
        <Select
          label={t("documents.category")}
          value={category}
          onChange={(event) => setCategory(event.target.value as DocumentCategory | "")}
        >
          <option value="">{t("documents.allCategories")}</option>
          {CATEGORIES.map((value) => (
            <option value={value} key={value}>{t(`documents.categories.${value}`)}</option>
          ))}
        </Select>
      </div>

      {documents.data?.length === 0 ? (
        <div className="empty-state">{t("documents.noDocuments")}</div>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("documents.documentTitle")}</th>
                <th>{t("documents.category")}</th>
                <th>{t("documents.visibility")}</th>
                <th>{t("documents.version")}</th>
                <th>{t("documents.updated")}</th>
                <th>{t("documents.file")}</th>
              </tr>
            </thead>
            <tbody>
              {documents.data?.map((document) => (
                <tr key={document.id}>
                  <td data-label={t("documents.documentTitle")}>
                    <Link to={`/communities/${communityId}/documents/${document.id}`}>
                      {document.title}
                    </Link>
                  </td>
                  <td data-label={t("documents.category")}>
                    {t(`documents.categories.${document.category}`)}
                  </td>
                  <td data-label={t("documents.visibility")}>
                    <span className="badge">
                      {t(`documents.visibilities.${document.visibility}`)}
                    </span>
                  </td>
                  <td data-label={t("documents.version")}>v{document.currentVersionNumber ?? 1}</td>
                  <td data-label={t("documents.updated")}>
                    {formatDate(document.updatedAt, language)}
                  </td>
                  <td data-label={t("documents.file")}>
                    <div className="documents-file-cell">
                      <span>{document.fileName}</span>
                      {document.sizeBytes ? (
                        <small>{formatFileSize(document.sizeBytes, language)}</small>
                      ) : null}
                      {document.downloadUrl && document.fileName ? (
                        <Button
                          type="button"
                          variant="ghost"
                          className="btn--small"
                          onClick={() => void handleDownload(document.downloadUrl!, document.fileName!)}
                        >
                          {t("documents.download")}
                        </Button>
                      ) : document.fileName ? (
                        <small className="documents-file-missing">{t("documents.fileMissing")}</small>
                      ) : null}
                    </div>
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

function DocumentForm({ communityId, onDone }: { communityId: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useCreateDocument(communityId);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState<DocumentCategory>("ACTA");
  const [visibility, setVisibility] = useState<DocumentVisibility>("COMMUNITY");
  const [file, setFile] = useState<File | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) {
      return;
    }
    const formData = new FormData();
    formData.append("title", title);
    formData.append("description", description);
    formData.append("category", category);
    formData.append("visibility", visibility);
    formData.append("file", file);
    try {
      await mutation.mutateAsync(formData);
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("documents.newDocument")}</h3>
      {mutation.isError ? <Alert variant="error">{t("documents.uploadError")}</Alert> : null}
      <div className="form-grid">
        <Input
          label={t("documents.documentTitle")}
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          required
        />
        <Select
          label={t("documents.category")}
          value={category}
          onChange={(event) => setCategory(event.target.value as DocumentCategory)}
        >
          {CATEGORIES.map((value) => (
            <option value={value} key={value}>{t(`documents.categories.${value}`)}</option>
          ))}
        </Select>
        <Select
          label={t("documents.visibility")}
          value={visibility}
          onChange={(event) => setVisibility(event.target.value as DocumentVisibility)}
        >
          {VISIBILITIES.map((value) => (
            <option value={value} key={value}>{t(`documents.visibilities.${value}`)}</option>
          ))}
        </Select>
        <Input
          label={t("documents.file")}
          type="file"
          accept=".pdf,.jpg,.jpeg,.png,.webp,.docx,.xlsx,.txt"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          required
        />
        <Textarea
          label={t("documents.description")}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />
      </div>
      <p className="field__hint">{t("documents.fileTypes")}</p>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending} disabled={!file}>
          {t("documents.upload")}
        </Button>
      </div>
    </form>
  );
}

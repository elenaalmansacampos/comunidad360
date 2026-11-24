import { useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { DocumentViewer } from "../components/DocumentViewer";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { Textarea } from "../components/Textarea";
import { useAuthStore } from "../features/auth/authStore";
import { ApiError } from "../services/apiClient";
import { canPreviewDocument, useDocumentPreview } from "../features/documents/preview";
import {
  useAddDocumentVersion,
  useArchiveDocument,
  useDeleteDocumentPermanent,
  useDocument,
  useUpdateDocument,
} from "../features/documents/queries";
import {
  canManageDocuments,
  canUploadDocuments,
  downloadDocument,
  formatFileSize,
} from "../features/documents/utils";
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

export function DocumentDetailPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { communityId, documentId } = useParams<{ communityId: string; documentId: string }>();
  const user = useAuthStore((state) => state.user);
  const membership = user?.memberships.find((item) => item.id === communityId);
  const roles = membership?.roles ?? [];
  const canManage = canManageDocuments(roles);
  const canUpload = canUploadDocuments(roles);
  const language = normalizeLanguage(i18n.language);
  const detail = useDocument(communityId, documentId);
  const updateDocument = useUpdateDocument(communityId, documentId);
  const addVersion = useAddDocumentVersion(communityId, documentId);
  const archiveDocument = useArchiveDocument(communityId, documentId);
  const deleteDocument = useDeleteDocumentPermanent(communityId);
  const preview = useDocumentPreview();
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState<DocumentCategory>("ACTA");
  const [visibility, setVisibility] = useState<DocumentVisibility>("COMMUNITY");
  const [versionFile, setVersionFile] = useState<File | null>(null);
  const [downloadError, setDownloadError] = useState<"NONE" | "MISSING" | "GENERIC">("NONE");
  const [deleteError, setDeleteError] = useState<string | null>(null);

  if (detail.isLoading) {
    return <FullPageLoader />;
  }

  if (detail.isError || !detail.data) {
    return (
      <div className="page">
        <Alert variant="error">{t("documents.error")}</Alert>
        <Link to={`/communities/${communityId}/documents`}>{t("documents.backToList")}</Link>
      </div>
    );
  }

  const document = detail.data.document;
  const versions = detail.data.versions;

  function startEditing() {
    setTitle(document.title);
    setDescription(document.description ?? "");
    setCategory(document.category);
    setVisibility(document.visibility);
    setEditing(true);
  }

  async function saveMetadata(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await updateDocument.mutateAsync({
        title,
        description,
        category,
        visibility,
        version: document.version,
      });
      setEditing(false);
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function uploadVersion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!versionFile) return;
    const formData = new FormData();
    formData.append("file", versionFile);
    try {
      await addVersion.mutateAsync(formData);
      setVersionFile(null);
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function archive() {
    if (!window.confirm(t("documents.archiveConfirm"))) return;
    try {
      await archiveDocument.mutateAsync();
      navigate(`/communities/${communityId}/documents`);
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function removePermanently() {
    if (!window.confirm(t("documents.deleteConfirm"))) {
      return;
    }
    setDeleteError(null);
    try {
      await deleteDocument.mutateAsync(document.id);
      navigate(`/communities/${communityId}/documents`);
    } catch (error) {
      setDeleteError(
        error instanceof ApiError && error.code === "DOCUMENT_IN_USE"
          ? t("documents.deleteInUse")
          : t("documents.deleteError"),
      );
    }
  }

  async function download(url: string, fileName: string) {
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
          <Link className="breadcrumb" to={`/communities/${communityId}/documents`}>
            {t("documents.backToList")}
          </Link>
          <h1>{document.title}</h1>
          <div className="chips">
            <span className="badge">{t(`documents.categories.${document.category}`)}</span>
            <span className="badge">{t(`documents.visibilities.${document.visibility}`)}</span>
            <span className="badge">{t(`documents.statuses.${document.status}`)}</span>
          </div>
        </div>
        <div className="page-header__actions">
          {document.downloadUrl && document.fileName
            && canPreviewDocument(document.mimeType, document.fileName) ? (
            <Button variant="secondary" loading={preview.loading} onClick={() => void preview.open(document)}>
              {t("documents.view")}
            </Button>
          ) : null}
          {document.downloadUrl && document.fileName ? (
            <Button
              variant="secondary"
              onClick={() => void download(document.downloadUrl!, document.fileName!)}
            >
              {t("documents.downloadCurrent")}
            </Button>
          ) : null}
          {canManage ? (
            <>
              <Button variant="secondary" onClick={startEditing}>{t("common.edit")}</Button>
              <Button variant="ghost" onClick={() => void archive()} loading={archiveDocument.isPending}>
                {t("documents.archive")}
              </Button>
              <Button
                variant="ghost"
                className="btn--danger"
                onClick={() => void removePermanently()}
                loading={deleteDocument.isPending}
              >
                {t("documents.delete")}
              </Button>
            </>
          ) : null}
        </div>
      </header>

      {deleteError ? <Alert variant="error">{deleteError}</Alert> : null}
      {preview.error ? <Alert variant="error">{t("documents.previewError")}</Alert> : null}
      {downloadError !== "NONE" ? (
        <Alert variant="error">
          {downloadError === "MISSING" ? t("documents.fileMissing") : t("documents.downloadError")}
        </Alert>
      ) : null}
      {archiveDocument.isError ? <Alert variant="error">{t("documents.updateError")}</Alert> : null}

      <section className="detail-grid">
        <article className="detail-card">
          <h2>{t("documents.metadata")}</h2>
          <dl className="detail-list">
            <dt>{t("documents.category")}</dt>
            <dd>{t(`documents.categories.${document.category}`)}</dd>
            <dt>{t("documents.visibility")}</dt>
            <dd>{t(`documents.visibilities.${document.visibility}`)}</dd>
            <dt>{t("documents.version")}</dt>
            <dd>v{document.currentVersionNumber ?? 1}</dd>
            <dt>{t("documents.updated")}</dt>
            <dd>{formatDate(document.updatedAt, language)}</dd>
          </dl>
          {document.description ? <p className="detail-description">{document.description}</p> : null}
        </article>

        <article className="detail-card">
          <h2>{t("documents.currentFile")}</h2>
          {document.fileName ? (
            <>
              <div className="documents-current-file">
                <strong>{document.fileName}</strong>
                <span>{document.mimeType}</span>
                {document.sizeBytes ? <span>{formatFileSize(document.sizeBytes, language)}</span> : null}
              </div>
              {!document.downloadUrl ? (
                <Alert variant="error">{t("documents.fileMissing")}</Alert>
              ) : null}
            </>
          ) : (
            <p>{t("common.empty")}</p>
          )}
        </article>
      </section>

      {editing ? (
        <form className="form-card" onSubmit={saveMetadata}>
          <h3>{t("documents.editMetadata")}</h3>
          {updateDocument.isError ? <Alert variant="error">{t("documents.updateError")}</Alert> : null}
          <div className="form-grid">
            <Input label={t("documents.documentTitle")} value={title} onChange={(event) => setTitle(event.target.value)} required />
            <Select label={t("documents.category")} value={category} onChange={(event) => setCategory(event.target.value as DocumentCategory)}>
              {CATEGORIES.map((value) => (
                <option value={value} key={value}>{t(`documents.categories.${value}`)}</option>
              ))}
            </Select>
            <Select label={t("documents.visibility")} value={visibility} onChange={(event) => setVisibility(event.target.value as DocumentVisibility)}>
              {VISIBILITIES.map((value) => (
                <option value={value} key={value}>{t(`documents.visibilities.${value}`)}</option>
              ))}
            </Select>
            <Textarea label={t("documents.description")} value={description} onChange={(event) => setDescription(event.target.value)} />
          </div>
          <div className="form-actions">
            <Button type="submit" loading={updateDocument.isPending}>{t("common.save")}</Button>
            <Button type="button" variant="ghost" onClick={() => setEditing(false)}>{t("common.cancel")}</Button>
          </div>
        </form>
      ) : null}

      {canUpload ? (
        <form className="form-card" onSubmit={uploadVersion}>
          <h3>{t("documents.newVersion")}</h3>
          {addVersion.isError ? <Alert variant="error">{t("documents.uploadError")}</Alert> : null}
          <div className="form-grid">
            <Input
              label={t("documents.file")}
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.webp,.docx,.xlsx,.txt"
              onChange={(event) => setVersionFile(event.target.files?.[0] ?? null)}
              required
            />
          </div>
          <p className="field__hint">{t("documents.fileTypes")}</p>
          <div className="form-actions">
            <Button type="submit" loading={addVersion.isPending} disabled={!versionFile}>
              {t("documents.uploadVersion")}
            </Button>
          </div>
        </form>
      ) : null}

      <section className="detail-card">
        <h2>{t("documents.versions")}</h2>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("documents.version")}</th>
                <th>{t("documents.file")}</th>
                <th>{t("documents.uploadedBy")}</th>
                <th>{t("documents.uploadedAt")}</th>
                <th>{t("documents.size")}</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {versions.map((version) => (
                <tr key={version.id}>
                  <td data-label={t("documents.version")}>v{version.versionNumber}</td>
                  <td data-label={t("documents.file")}>{version.fileName}</td>
                  <td data-label={t("documents.uploadedBy")}>{version.uploadedByName ?? "—"}</td>
                  <td data-label={t("documents.uploadedAt")}>{formatDate(version.uploadedAt, language)}</td>
                  <td data-label={t("documents.size")}>{formatFileSize(version.sizeBytes, language)}</td>
                  <td>
                    {version.downloadUrl ? (
                      <Button
                        type="button"
                        variant="ghost"
                        className="btn--small"
                        onClick={() => void download(version.downloadUrl!, version.fileName)}
                      >
                        {t("documents.download")}
                      </Button>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <DocumentViewer preview={preview.preview} onClose={preview.close} />
    </div>
  );
}

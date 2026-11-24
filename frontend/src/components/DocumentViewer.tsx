import { useEffect } from "react";
import { createPortal } from "react-dom";
import { Download, ExternalLink, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import type { DocumentPreview } from "../features/documents/preview";

export function DocumentViewer({
  preview,
  onClose,
}: {
  preview: DocumentPreview | null;
  onClose: () => void;
}) {
  const { t } = useTranslation();

  useEffect(() => {
    if (!preview) {
      return;
    }
    function handleKey(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onClose, preview]);

  if (!preview) {
    return null;
  }

  const isImage = preview.mimeType.toLowerCase().startsWith("image/");

  return createPortal(
    <div
      className="document-viewer"
      role="dialog"
      aria-modal="true"
      aria-label={preview.fileName}
      onClick={onClose}
    >
      <div className="document-viewer__panel" onClick={(event) => event.stopPropagation()}>
        <header className="document-viewer__header">
          <strong>{preview.fileName}</strong>
          <div className="document-viewer__actions">
            <a className="btn btn--ghost btn--small" href={preview.objectUrl} download={preview.fileName}>
              <Download size={14} aria-hidden="true" /> {t("documents.download")}
            </a>
            <a
              className="btn btn--ghost btn--small"
              href={preview.objectUrl}
              target="_blank"
              rel="noopener noreferrer"
            >
              <ExternalLink size={14} aria-hidden="true" /> {t("documents.openInTab")}
            </a>
            <button
              type="button"
              className="icon-btn"
              onClick={onClose}
              aria-label={t("common.close")}
              title={t("common.close")}
            >
              <X size={18} aria-hidden="true" />
            </button>
          </div>
        </header>
        <div className="document-viewer__body">
          {isImage ? (
            <img src={preview.objectUrl} alt={preview.fileName} />
          ) : (
            <iframe src={preview.objectUrl} title={preview.fileName} />
          )}
        </div>
      </div>
    </div>,
    document.body,
  );
}

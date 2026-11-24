import { useEffect, useState } from "react";
import { downloadFile } from "../../services/apiClient";

export interface DocumentPreview {
  objectUrl: string;
  fileName: string;
  mimeType: string;
  downloadUrl: string;
}

export function canPreviewDocument(mimeType?: string | null, fileName?: string | null): boolean {
  const type = (mimeType ?? "").toLowerCase();
  const name = (fileName ?? "").toLowerCase();
  return type === "application/pdf"
    || type.startsWith("image/")
    || type === "text/plain"
    || name.endsWith(".pdf")
    || /\.(jpe?g|png|webp)$/.test(name)
    || name.endsWith(".txt");
}

export function useDocumentPreview() {
  const [preview, setPreview] = useState<DocumentPreview | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => () => {
    if (preview) {
      URL.revokeObjectURL(preview.objectUrl);
    }
  }, [preview]);

  async function open(document: {
    downloadUrl?: string | null;
    fileName?: string | null;
    mimeType?: string | null;
  }) {
    if (!document.downloadUrl || !document.fileName) {
      return;
    }
    setError(false);
    setLoading(true);
    setPreview(null);
    try {
      const blob = await downloadFile(document.downloadUrl);
      const mimeType = document.mimeType || blob.type || "application/octet-stream";
      setPreview({
        objectUrl: URL.createObjectURL(blob),
        fileName: document.fileName,
        mimeType,
        downloadUrl: document.downloadUrl,
      });
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }

  return {
    preview,
    loading,
    error,
    open,
    close: () => setPreview(null),
  };
}

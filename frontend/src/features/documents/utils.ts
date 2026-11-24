import { downloadFile } from "../../services/apiClient";
import type { Language, Role } from "../../types/api";
import { localeFor } from "../../utils/language";

const MANAGE_ROLES: Role[] = ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY"];
const UPLOAD_ROLES: Role[] = [
  "COMMUNITY_ADMIN",
  "PRESIDENT",
  "SECRETARY",
  "TREASURER",
  "OWNER",
];

export function canManageDocuments(roles: Role[]): boolean {
  return roles.some((role) => MANAGE_ROLES.includes(role));
}

export function canUploadDocuments(roles: Role[]): boolean {
  return roles.some((role) => UPLOAD_ROLES.includes(role));
}

export function isExternalUrl(url: string): boolean {
  return /^https?:\/\//i.test(url);
}

export function formatFileSize(bytes: number, language: Language): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const units = ["KB", "MB", "GB"];
  let value = bytes / 1024;
  let unit = units[0];
  for (let index = 1; index < units.length && value >= 1024; index += 1) {
    value /= 1024;
    unit = units[index];
  }
  return `${new Intl.NumberFormat(localeFor(language), {
    maximumFractionDigits: 1,
  }).format(value)} ${unit}`;
}

export async function downloadDocument(url: string, fileName: string): Promise<void> {
  if (isExternalUrl(url)) {
    window.open(url, "_blank", "noopener,noreferrer");
    return;
  }
  const blob = await downloadFile(url);
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = fileName;
  link.rel = "noopener";
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
}

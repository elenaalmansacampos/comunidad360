import type { Role } from "../../types/api";

export function canManageProviders(roles: Role[]): boolean {
  return roles.some((role) => ["COMMUNITY_ADMIN", "TREASURER"].includes(role));
}

export function canViewProviders(roles: Role[]): boolean {
  return roles.some((role) => ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY", "TREASURER"].includes(role));
}

export function canManageVotings(roles: Role[]): boolean {
  return roles.some((role) => ["COMMUNITY_ADMIN", "SECRETARY"].includes(role));
}

export function canModerateChat(roles: Role[]): boolean {
  return roles.some((role) => ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY"].includes(role));
}

export function formatChatTime(value: string, language: "es" | "ca"): string {
  return new Intl.DateTimeFormat(language === "ca" ? "ca-ES" : "es-ES", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

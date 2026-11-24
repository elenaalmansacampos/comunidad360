import type { Language, Role } from "../../types/api";
import { localeFor } from "../../utils/language";

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

export function formatChatTime(value: string, language: Language): string {
  return new Intl.DateTimeFormat(localeFor(language), {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function canManageBoard(roles: Role[]): boolean {
  return roles.some((role) => ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY"].includes(role));
}

export function canManageLostFound(roles: Role[]): boolean {
  return roles.some((role) => ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY"].includes(role));
}

export function isReadOnly(roles: Role[]): boolean {
  return roles.includes("VIEWER");
}

export function canManageStructure(roles: Role[]): boolean {
  return roles.includes("COMMUNITY_ADMIN");
}

export function canManageWorkPermits(roles: Role[]): boolean {
  return roles.some((role) => ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY"].includes(role));
}

export function canCreateWorkPermits(roles: Role[]): boolean {
  return roles.some((role) =>
    ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY", "TREASURER", "OWNER", "TENANT"].includes(role));
}

export function canCastVotes(roles: Role[]): boolean {
  return roles.some((role) =>
    ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY", "TREASURER", "OWNER", "TENANT"].includes(role));
}

export function canCreateAuthorizations(roles: Role[]): boolean {
  return roles.some((role) =>
    ["SUPER_ADMIN", "COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY", "TREASURER",
      "OWNER", "TENANT", "VECINO"].includes(role));
}

export function canManageAuthorizations(roles: Role[]): boolean {
  return roles.some((role) =>
    ["SUPER_ADMIN", "COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY"].includes(role));
}

export function canCreateReservations(roles: Role[]): boolean {
  return roles.some((role) =>
    ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY", "TREASURER", "OWNER", "TENANT", "VECINO"].includes(role));
}

export function canManageReservations(roles: Role[]): boolean {
  return roles.some((role) => ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY"].includes(role));
}

export function canManageAreas(roles: Role[]): boolean {
  return canManageReservations(roles);
}

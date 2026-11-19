import type { IncidentStatus, Role } from "../../types/api";

const MANAGE_ROLES: Role[] = ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY"];

const TRANSITIONS: Record<IncidentStatus, IncidentStatus[]> = {
  OPEN: ["IN_PROGRESS", "WAITING_PROVIDER", "CANCELLED"],
  IN_PROGRESS: ["WAITING_PROVIDER", "RESOLVED", "CANCELLED"],
  WAITING_PROVIDER: ["IN_PROGRESS", "RESOLVED", "CANCELLED"],
  RESOLVED: ["CLOSED", "IN_PROGRESS"],
  CLOSED: [],
  CANCELLED: [],
};

export function canManageIncidents(roles: Role[]): boolean {
  return roles.some((role) => MANAGE_ROLES.includes(role));
}

export function isActiveIncident(status: IncidentStatus): boolean {
  return status !== "CLOSED" && status !== "CANCELLED";
}

export function nextIncidentStatuses(status: IncidentStatus): IncidentStatus[] {
  return TRANSITIONS[status];
}

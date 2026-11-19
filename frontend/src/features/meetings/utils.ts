import type { MeetingStatus, Role } from "../../types/api";

const MANAGE_ROLES: Role[] = ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY"];

export function canManageMeetings(roles: Role[]): boolean {
  return roles.some((role) => MANAGE_ROLES.includes(role));
}

export function nextMeetingStatuses(status: MeetingStatus): MeetingStatus[] {
  switch (status) {
    case "DRAFT":
      return ["CONVENED", "CANCELLED"];
    case "CONVENED":
      return ["HELD", "CANCELLED"];
    case "HELD":
    case "CANCELLED":
      return [];
  }
}

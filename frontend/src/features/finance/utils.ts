import type { AssessmentCharge, Role } from "../../types/api";

const STAFF_ROLES: Role[] = ["COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY", "TREASURER"];
const MANAGE_ROLES: Role[] = ["COMMUNITY_ADMIN", "TREASURER"];

export function isFinanceStaff(roles: Role[]): boolean {
  return roles.some((role) => STAFF_ROLES.includes(role));
}

export function canManageFinance(roles: Role[]): boolean {
  return roles.some((role) => MANAGE_ROLES.includes(role));
}

export function pendingChargeAmount(charge: AssessmentCharge): number {
  return Math.max(0, Number(charge.amount) - Number(charge.paidAmount));
}

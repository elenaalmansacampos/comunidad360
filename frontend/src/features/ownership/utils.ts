import type { Ownership } from "../../types/api";

export function activeOwnerships(ownerships: Ownership[]): Ownership[] {
  return ownerships.filter((ownership) => ownership.active);
}

export function sumActiveShares(ownerships: Ownership[]): number {
  return ownerships
    .filter((ownership) => ownership.active)
    .reduce((total, ownership) => total + Number(ownership.sharePercentage), 0);
}

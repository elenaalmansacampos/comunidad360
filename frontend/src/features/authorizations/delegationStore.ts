import { create } from "zustand";
import type { AuthorizationScope } from "../../types/api";

export interface ActiveDelegation {
  communityId: string;
  membershipId: string;
  scope: AuthorizationScope;
  name: string;
}

interface DelegationState {
  active: ActiveDelegation | null;
  setActive: (value: ActiveDelegation | null) => void;
  clear: () => void;
}

export const useDelegationStore = create<DelegationState>((set) => ({
  active: null,
  setActive: (active) => set({ active }),
  clear: () => set({ active: null }),
}));

export function actingQuery(communityId: string | undefined, scope: AuthorizationScope): string {
  const active = useDelegationStore.getState().active;
  if (!communityId || !active || active.communityId !== communityId || active.scope !== scope) {
    return "";
  }
  return `?onBehalfOfMembershipId=${encodeURIComponent(active.membershipId)}`;
}

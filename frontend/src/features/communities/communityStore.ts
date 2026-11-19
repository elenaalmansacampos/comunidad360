import { create } from "zustand";
import { persist } from "zustand/middleware";

interface CommunityState {
  selectedCommunityId: string | null;
  setSelectedCommunityId: (communityId: string | null) => void;
}

export const useCommunityStore = create<CommunityState>()(
  persist(
    (set) => ({
      selectedCommunityId: null,
      setSelectedCommunityId: (communityId) => set({ selectedCommunityId: communityId }),
    }),
    {
      name: "comunidad360.community",
    },
  ),
);

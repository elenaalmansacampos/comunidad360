import { useQuery } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { CommunitySummary, Dashboard, Member } from "../../types/api";
import { useAuthStore } from "../auth/authStore";

export function useCommunities() {
  const status = useAuthStore((state) => state.status);
  return useQuery({
    queryKey: ["communities"],
    queryFn: () => api.get<CommunitySummary[]>("/communities"),
    enabled: status === "authenticated",
  });
}

export function useDashboard(communityId: string | undefined) {
  return useQuery({
    queryKey: ["dashboard", communityId],
    queryFn: () => api.get<Dashboard>(`/communities/${communityId}/dashboard`),
    enabled: Boolean(communityId),
  });
}

export function useMembers(communityId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: ["members", communityId],
    queryFn: () => api.get<Member[]>(`/communities/${communityId}/members`),
    enabled: Boolean(communityId) && enabled,
  });
}

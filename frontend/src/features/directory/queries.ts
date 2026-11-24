import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { DirectoryEntry } from "../../types/api";

function key(communityId: string | undefined) {
  return ["directory", communityId] as const;
}

export function useDirectory(communityId: string | undefined) {
  return useQuery({
    queryKey: key(communityId),
    queryFn: () => api.get<DirectoryEntry[]>(`/communities/${communityId}/directory`),
    enabled: Boolean(communityId),
  });
}

export function useUpdateDirectoryProfile(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      displayName?: string; unitLabel?: string; phone?: string;
      showEmail?: boolean; showPhone?: boolean;
    }) =>
      api.put<DirectoryEntry>(`/communities/${communityId}/directory/me`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

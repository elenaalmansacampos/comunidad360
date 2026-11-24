import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { LostItem, LostItemStatus, LostItemType } from "../../types/api";

function key(communityId: string | undefined) {
  return ["lost-items", communityId] as const;
}

export function useLostItems(communityId: string | undefined, type?: LostItemType) {
  const query = type ? `?type=${type}` : "";
  return useQuery({
    queryKey: [...key(communityId), type ?? "ALL"],
    queryFn: () => api.get<LostItem[]>(`/communities/${communityId}/lost-items${query}`),
    enabled: Boolean(communityId),
  });
}

export function useCreateLostItem(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      title: string; description?: string; type: LostItemType; location?: string;
      eventDate?: string; contact?: string;
    }) => api.post<LostItem>(`/communities/${communityId}/lost-items`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useUpdateLostItem(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, ...payload }: {
      itemId: string; title?: string; description?: string; type?: LostItemType;
      location?: string; eventDate?: string; contact?: string; status?: LostItemStatus;
      version?: number;
    }) => api.patch<LostItem>(`/communities/${communityId}/lost-items/${itemId}`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useDeleteLostItem(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (itemId: string) => api.delete<void>(`/communities/${communityId}/lost-items/${itemId}`),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useUploadLostItemPhoto(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, file }: { itemId: string; file: File }) => {
      const formData = new FormData();
      formData.append("file", file);
      return api.post<LostItem>(`/communities/${communityId}/lost-items/${itemId}/photo`, formData);
    },
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

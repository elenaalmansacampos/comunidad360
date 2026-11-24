import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { actingQuery } from "../authorizations/delegationStore";
import { api } from "../../services/apiClient";
import type { LostItem, LostItemAttachment, LostItemNote, LostItemStatus, LostItemType } from "../../types/api";

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
    }) => api.post<LostItem>(`/communities/${communityId}/lost-items${actingQuery(communityId, "LOST_FOUND")}`, payload),
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
      return api.post<LostItem>(`/communities/${communityId}/lost-items/${itemId}/photo${actingQuery(communityId, "LOST_FOUND")}`, formData);
    },
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useAddLostItemNote(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, body }: { itemId: string; body: string }) =>
      api.post<LostItemNote>(
        `/communities/${communityId}/lost-items/${itemId}/notes${actingQuery(communityId, "LOST_FOUND")}`, { body }),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useDeleteLostItemNote(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, noteId }: { itemId: string; noteId: string }) =>
      api.delete<void>(`/communities/${communityId}/lost-items/${itemId}/notes/${noteId}`),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useUploadLostItemAttachment(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, file }: { itemId: string; file: File }) => {
      const formData = new FormData();
      formData.append("file", file);
      return api.post<LostItemAttachment>(
        `/communities/${communityId}/lost-items/${itemId}/attachments${actingQuery(communityId, "LOST_FOUND")}`,
        formData);
    },
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useDeleteLostItemAttachment(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, attachmentId }: { itemId: string; attachmentId: string }) =>
      api.delete<void>(`/communities/${communityId}/lost-items/${itemId}/attachments/${attachmentId}`),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { BoardNote, BoardNoteColor } from "../../types/api";

function key(communityId: string | undefined) {
  return ["board-notes", communityId] as const;
}

export function useBoardNotes(communityId: string | undefined) {
  return useQuery({
    queryKey: key(communityId),
    queryFn: () => api.get<BoardNote[]>(`/communities/${communityId}/board-notes`),
    enabled: Boolean(communityId),
  });
}

export function useCreateBoardNote(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { title: string; body: string; color: BoardNoteColor }) =>
      api.post<BoardNote>(`/communities/${communityId}/board-notes`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useUpdateBoardNote(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ noteId, ...payload }: {
      noteId: string; title?: string; body?: string; color?: BoardNoteColor;
      pinned?: boolean; version?: number;
    }) => api.patch<BoardNote>(`/communities/${communityId}/board-notes/${noteId}`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useDeleteBoardNote(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (noteId: string) => api.delete<void>(`/communities/${communityId}/board-notes/${noteId}`),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

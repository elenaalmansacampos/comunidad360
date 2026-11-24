import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { actingQuery } from "../authorizations/delegationStore";
import { api } from "../../services/apiClient";
import type { BoardNote, BoardNoteColor, BoardNoteComment } from "../../types/api";

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
      api.post<BoardNote>(`/communities/${communityId}/board-notes${actingQuery(communityId, "BOARD")}`, payload),
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

export function useBoardNoteComments(communityId: string | undefined, noteId: string | null) {
  return useQuery({
    queryKey: ["board-note-comments", communityId, noteId],
    queryFn: () => api.get<BoardNoteComment[]>(
      `/communities/${communityId}/board-notes/${noteId}/comments`),
    enabled: Boolean(communityId) && Boolean(noteId),
  });
}

export function useAddBoardNoteComment(communityId: string | undefined, noteId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: string) => api.post<BoardNoteComment>(
      `/communities/${communityId}/board-notes/${noteId}/comments${actingQuery(communityId, "BOARD")}`, { body }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["board-note-comments", communityId, noteId] });
      void queryClient.invalidateQueries({ queryKey: key(communityId) });
    },
  });
}

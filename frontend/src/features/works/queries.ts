import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { WorkPermit, WorkPermitStatus } from "../../types/api";

function key(communityId: string | undefined) {
  return ["work-permits", communityId] as const;
}

export function useWorkPermits(communityId: string | undefined) {
  return useQuery({
    queryKey: key(communityId),
    queryFn: () => api.get<WorkPermit[]>(`/communities/${communityId}/work-permits`),
    enabled: Boolean(communityId),
  });
}

export function useCreateWorkPermit(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      title: string; description?: string; location?: string; startDate?: string; endDate?: string;
    }) => api.post<WorkPermit>(`/communities/${communityId}/work-permits`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useUpdateWorkPermit(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ permitId, ...payload }: {
      permitId: string; title?: string; description?: string; location?: string;
      startDate?: string; endDate?: string; status?: WorkPermitStatus;
      reviewNotes?: string; version?: number;
    }) => api.patch<WorkPermit>(`/communities/${communityId}/work-permits/${permitId}`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useUploadWorkPermitFile(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ permitId, file }: { permitId: string; file: File }) => {
      const formData = new FormData();
      formData.append("file", file);
      return api.post<WorkPermit>(`/communities/${communityId}/work-permits/${permitId}/file`, formData);
    },
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useDeleteWorkPermit(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (permitId: string) => api.delete<void>(`/communities/${communityId}/work-permits/${permitId}`),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

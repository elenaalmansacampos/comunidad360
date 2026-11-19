import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type {
  CommunityDocument,
  DocumentCategory,
  DocumentDetail,
  DocumentStatus,
  DocumentVisibility,
} from "../../types/api";

function documentsKey(communityId: string | undefined) {
  return ["documents", communityId] as const;
}

function documentKey(communityId: string | undefined, documentId: string | undefined) {
  return ["document", communityId, documentId] as const;
}

function invalidateDocument(queryClient: ReturnType<typeof useQueryClient>,
                            communityId: string | undefined, documentId?: string) {
  void queryClient.invalidateQueries({ queryKey: documentsKey(communityId) });
  if (documentId) {
    void queryClient.invalidateQueries({ queryKey: documentKey(communityId, documentId) });
  }
}

export function useDocuments(communityId: string | undefined, category?: DocumentCategory) {
  return useQuery({
    queryKey: [...documentsKey(communityId), category ?? "all"],
    queryFn: () => {
      const query = category ? `?category=${category}` : "";
      return api.get<CommunityDocument[]>(`/communities/${communityId}/documents${query}`);
    },
    enabled: Boolean(communityId),
  });
}

export function useDocument(communityId: string | undefined, documentId: string | undefined) {
  return useQuery({
    queryKey: documentKey(communityId, documentId),
    queryFn: () => api.get<DocumentDetail>(`/communities/${communityId}/documents/${documentId}`),
    enabled: Boolean(communityId) && Boolean(documentId),
  });
}

export function useCreateDocument(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) =>
      api.post<CommunityDocument>(`/communities/${communityId}/documents`, formData),
    onSuccess: () => invalidateDocument(queryClient, communityId),
  });
}

export function useAddDocumentVersion(communityId: string | undefined, documentId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) =>
      api.post<CommunityDocument>(
        `/communities/${communityId}/documents/${documentId}/versions`,
        formData,
      ),
    onSuccess: () => invalidateDocument(queryClient, communityId, documentId),
  });
}

export function useUpdateDocument(communityId: string | undefined, documentId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      title?: string;
      description?: string;
      category?: DocumentCategory;
      visibility?: DocumentVisibility;
      status?: DocumentStatus;
      version?: number;
    }) => api.patch<CommunityDocument>(`/communities/${communityId}/documents/${documentId}`, payload),
    onSuccess: () => invalidateDocument(queryClient, communityId, documentId),
  });
}

export function useArchiveDocument(communityId: string | undefined, documentId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.delete<void>(`/communities/${communityId}/documents/${documentId}`),
    onSuccess: () => invalidateDocument(queryClient, communityId, documentId),
  });
}

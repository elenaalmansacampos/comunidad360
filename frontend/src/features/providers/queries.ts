import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { Provider, ProviderContact, ProviderStatus } from "../../types/api";

function providersKey(communityId: string | undefined) { return ["providers", communityId] as const; }
function providerKey(communityId: string | undefined, providerId: string | undefined) {
  return ["provider", communityId, providerId] as const;
}

export function useProviders(communityId: string | undefined) {
  return useQuery({
    queryKey: providersKey(communityId),
    queryFn: () => api.get<Provider[]>(`/communities/${communityId}/providers`),
    enabled: Boolean(communityId),
  });
}

export function useProvider(communityId: string | undefined, providerId: string | undefined) {
  return useQuery({
    queryKey: providerKey(communityId, providerId),
    queryFn: () => api.get<Provider>(`/communities/${communityId}/providers/${providerId}`),
    enabled: Boolean(communityId) && Boolean(providerId),
  });
}

export function useCreateProvider(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      name: string; taxId?: string; category?: string; email?: string;
      phone?: string; address?: string; notes?: string;
    }) => api.post<Provider>(`/communities/${communityId}/providers`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: providersKey(communityId) }); },
  });
}

export function useUpdateProvider(communityId: string | undefined, providerId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      name?: string; taxId?: string; category?: string; email?: string; phone?: string;
      address?: string; status?: ProviderStatus; notes?: string; version?: number;
    }) => api.patch<Provider>(`/communities/${communityId}/providers/${providerId}`, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: providersKey(communityId) });
      void queryClient.invalidateQueries({ queryKey: providerKey(communityId, providerId) });
    },
  });
}

export function useProviderContacts(communityId: string | undefined, providerId: string | undefined) {
  return useQuery({
    queryKey: ["provider-contacts", communityId, providerId],
    queryFn: () => api.get<ProviderContact[]>(`/communities/${communityId}/providers/${providerId}/contacts`),
    enabled: Boolean(communityId) && Boolean(providerId),
  });
}

export function useAddProviderContact(communityId: string | undefined, providerId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      name: string; role?: string; email?: string; phone?: string; primaryContact?: boolean;
    }) => api.post<ProviderContact>(
      `/communities/${communityId}/providers/${providerId}/contacts`, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["provider-contacts", communityId, providerId] });
    },
  });
}

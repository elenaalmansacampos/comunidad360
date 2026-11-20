import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type {
  Provider,
  ProviderBudget,
  ProviderBudgetStatus,
  ProviderContact,
  ProviderContract,
  ProviderContractStatus,
  ProviderHistoryEntry,
  ProviderNote,
  ProviderStatus,
} from "../../types/api";

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

function activityKey(kind: string, communityId: string | undefined, providerId: string | undefined) {
  return [kind, communityId, providerId] as const;
}

function invalidateActivity(
  queryClient: ReturnType<typeof useQueryClient>,
  communityId: string | undefined,
  providerId: string | undefined,
) {
  for (const kind of ["provider-contracts", "provider-budgets", "provider-notes", "provider-history"]) {
    void queryClient.invalidateQueries({ queryKey: activityKey(kind, communityId, providerId) });
  }
}

export function useProviderContracts(communityId: string | undefined, providerId: string | undefined) {
  return useQuery({
    queryKey: activityKey("provider-contracts", communityId, providerId),
    queryFn: () => api.get<ProviderContract[]>(
      `/communities/${communityId}/providers/${providerId}/contracts`),
    enabled: Boolean(communityId) && Boolean(providerId),
  });
}

export function useCreateProviderContract(communityId: string | undefined, providerId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      title: string; description?: string; startDate?: string; endDate?: string;
      amount?: number; status?: ProviderContractStatus;
    }) => api.post<ProviderContract>(
      `/communities/${communityId}/providers/${providerId}/contracts`, payload),
    onSuccess: () => invalidateActivity(queryClient, communityId, providerId),
  });
}

export function useUpdateProviderContract(communityId: string | undefined, providerId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ contractId, ...payload }: {
      contractId: string; title?: string; description?: string; startDate?: string; endDate?: string;
      amount?: number; status?: ProviderContractStatus; version?: number;
    }) => api.patch<ProviderContract>(
      `/communities/${communityId}/providers/${providerId}/contracts/${contractId}`, payload),
    onSuccess: () => invalidateActivity(queryClient, communityId, providerId),
  });
}

export function useProviderBudgets(communityId: string | undefined, providerId: string | undefined) {
  return useQuery({
    queryKey: activityKey("provider-budgets", communityId, providerId),
    queryFn: () => api.get<ProviderBudget[]>(
      `/communities/${communityId}/providers/${providerId}/budgets`),
    enabled: Boolean(communityId) && Boolean(providerId),
  });
}

export function useCreateProviderBudget(communityId: string | undefined, providerId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      title: string; description?: string; amount: number; validUntil?: string; status?: ProviderBudgetStatus;
    }) => api.post<ProviderBudget>(
      `/communities/${communityId}/providers/${providerId}/budgets`, payload),
    onSuccess: () => invalidateActivity(queryClient, communityId, providerId),
  });
}

export function useUpdateProviderBudget(communityId: string | undefined, providerId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ budgetId, ...payload }: {
      budgetId: string; title?: string; description?: string; amount?: number; validUntil?: string;
      status?: ProviderBudgetStatus; version?: number;
    }) => api.patch<ProviderBudget>(
      `/communities/${communityId}/providers/${providerId}/budgets/${budgetId}`, payload),
    onSuccess: () => invalidateActivity(queryClient, communityId, providerId),
  });
}

export function useProviderNotes(communityId: string | undefined, providerId: string | undefined) {
  return useQuery({
    queryKey: activityKey("provider-notes", communityId, providerId),
    queryFn: () => api.get<ProviderNote[]>(
      `/communities/${communityId}/providers/${providerId}/notes`),
    enabled: Boolean(communityId) && Boolean(providerId),
  });
}

export function useAddProviderNote(communityId: string | undefined, providerId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { body: string }) => api.post<ProviderNote>(
      `/communities/${communityId}/providers/${providerId}/notes`, payload),
    onSuccess: () => invalidateActivity(queryClient, communityId, providerId),
  });
}

export function useProviderHistory(communityId: string | undefined, providerId: string | undefined) {
  return useQuery({
    queryKey: activityKey("provider-history", communityId, providerId),
    queryFn: () => api.get<ProviderHistoryEntry[]>(
      `/communities/${communityId}/providers/${providerId}/history`),
    enabled: Boolean(communityId) && Boolean(providerId),
  });
}

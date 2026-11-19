import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { Ownership, OwnershipType, Tenancy } from "../../types/api";

export interface CreateOwnershipPayload {
  userId: string;
  ownershipType: OwnershipType;
  sharePercentage: string;
  validFrom: string;
  validTo?: string;
}

export interface CreateTenancyPayload {
  userId: string;
  validFrom: string;
  validTo?: string;
}

function ownershipKey(communityId: string | undefined, unitId: string | undefined) {
  return ["ownerships", communityId, unitId] as const;
}

function tenancyKey(communityId: string | undefined, unitId: string | undefined) {
  return ["tenancies", communityId, unitId] as const;
}

export function useOwnerships(communityId: string | undefined, unitId: string | undefined) {
  return useQuery({
    queryKey: ownershipKey(communityId, unitId),
    queryFn: () => api.get<Ownership[]>(`/communities/${communityId}/units/${unitId}/ownerships`),
    enabled: Boolean(communityId) && Boolean(unitId),
  });
}

export function useTenancies(communityId: string | undefined, unitId: string | undefined) {
  return useQuery({
    queryKey: tenancyKey(communityId, unitId),
    queryFn: () => api.get<Tenancy[]>(`/communities/${communityId}/units/${unitId}/tenancies`),
    enabled: Boolean(communityId) && Boolean(unitId),
  });
}

export function useCreateOwnership(communityId: string | undefined, unitId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateOwnershipPayload) =>
      api.post<Ownership>(`/communities/${communityId}/units/${unitId}/ownerships`, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ownershipKey(communityId, unitId) }),
  });
}

export function useEndOwnership(communityId: string | undefined, unitId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (ownershipId: string) =>
      api.delete<void>(`/communities/${communityId}/units/${unitId}/ownerships/${ownershipId}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ownershipKey(communityId, unitId) }),
  });
}

export function useCreateTenancy(communityId: string | undefined, unitId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateTenancyPayload) =>
      api.post<Tenancy>(`/communities/${communityId}/units/${unitId}/tenancies`, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tenancyKey(communityId, unitId) }),
  });
}

export function useRevokeTenancy(communityId: string | undefined, unitId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tenancyId: string) =>
      api.delete<void>(`/communities/${communityId}/units/${unitId}/tenancies/${tenancyId}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tenancyKey(communityId, unitId) }),
  });
}

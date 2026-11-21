import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type {
  Organization,
  OrganizationCommunity,
  OrganizationMember,
  OrganizationMemberRole,
} from "../../types/api";

const mineKey = ["organizations", "mine"] as const;
const organizationKey = (organizationId: string | undefined) => ["organization", organizationId] as const;
const communitiesKey = (organizationId: string | undefined) =>
  ["organization-communities", organizationId] as const;
const membersKey = (organizationId: string | undefined) =>
  ["organization-members", organizationId] as const;

export function useMyOrganizations() {
  return useQuery({
    queryKey: mineKey,
    queryFn: () => api.get<Organization[]>("/organizations/mine"),
  });
}

export function useCreateOrganization() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { name: string; taxId?: string }) =>
      api.post<Organization>("/organizations", payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: mineKey });
    },
  });
}

export function useOrganization(organizationId: string | undefined) {
  return useQuery({
    queryKey: organizationKey(organizationId),
    queryFn: () => api.get<Organization>(`/organizations/${organizationId}`),
    enabled: Boolean(organizationId),
  });
}

export function useOrganizationCommunities(organizationId: string | undefined) {
  return useQuery({
    queryKey: communitiesKey(organizationId),
    queryFn: () => api.get<OrganizationCommunity[]>(`/organizations/${organizationId}/communities`),
    enabled: Boolean(organizationId),
  });
}

export function useAttachCommunity(organizationId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (communityId: string) =>
      api.post<void>(`/organizations/${organizationId}/communities/${communityId}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: communitiesKey(organizationId) });
      void queryClient.invalidateQueries({ queryKey: organizationKey(organizationId) });
      void queryClient.invalidateQueries({ queryKey: mineKey });
    },
  });
}

export function useDetachCommunity(organizationId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (communityId: string) =>
      api.delete<void>(`/organizations/${organizationId}/communities/${communityId}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: communitiesKey(organizationId) });
      void queryClient.invalidateQueries({ queryKey: organizationKey(organizationId) });
      void queryClient.invalidateQueries({ queryKey: mineKey });
    },
  });
}

export function useOrganizationMembers(organizationId: string | undefined) {
  return useQuery({
    queryKey: membersKey(organizationId),
    queryFn: () => api.get<OrganizationMember[]>(`/organizations/${organizationId}/members`),
    enabled: Boolean(organizationId),
  });
}

export function useAddOrganizationMember(organizationId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { email: string; role: OrganizationMemberRole }) =>
      api.post<OrganizationMember>(`/organizations/${organizationId}/members`, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: membersKey(organizationId) });
      void queryClient.invalidateQueries({ queryKey: communitiesKey(organizationId) });
    },
  });
}

export function useRemoveOrganizationMember(organizationId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) =>
      api.delete<void>(`/organizations/${organizationId}/members/${userId}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: membersKey(organizationId) });
      void queryClient.invalidateQueries({ queryKey: communitiesKey(organizationId) });
    },
  });
}

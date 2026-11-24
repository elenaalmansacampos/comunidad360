import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { Authorization, AuthorizationScope, CreateAuthorizationPayload } from "../../types/api";

export type AuthorizationDirection = "ALL" | "GRANTED" | "RECEIVED";

function key(communityId: string | undefined) {
  return ["authorizations", communityId] as const;
}

export function useAuthorizations(communityId: string | undefined,
                                  direction: AuthorizationDirection = "ALL",
                                  all = false) {
  return useQuery({
    queryKey: [...key(communityId), direction, all],
    queryFn: () => {
      const params = new URLSearchParams({ direction });
      if (all) {
        params.set("all", "true");
      }
      return api.get<Authorization[]>(
        `/communities/${communityId}/authorizations?${params.toString()}`);
    },
    enabled: Boolean(communityId),
  });
}

export function useReceivedAuthorizations(communityId: string | undefined) {
  return useAuthorizations(communityId, "RECEIVED");
}

export function useCreateAuthorization(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateAuthorizationPayload) =>
      api.post<Authorization>(`/communities/${communityId}/authorizations`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useRevokeAuthorization(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (authorizationId: string) =>
      api.delete<void>(`/communities/${communityId}/authorizations/${authorizationId}`),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: key(communityId) }); },
  });
}

export function useHasDelegation(communityId: string | undefined, scope: AuthorizationScope): boolean {
  const query = useReceivedAuthorizations(communityId);
  return (query.data ?? []).some(
    (authorization) => authorization.effective
      && authorization.status === "ACTIVE"
      && authorization.scope === scope);
}

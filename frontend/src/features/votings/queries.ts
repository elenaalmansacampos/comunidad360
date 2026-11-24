import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { actingQuery } from "../authorizations/delegationStore";
import { api } from "../../services/apiClient";
import type { Voting, VotingDetail } from "../../types/api";

function votingsKey(communityId: string | undefined) { return ["votings", communityId] as const; }
function votingKey(communityId: string | undefined, votingId: string | undefined) {
  return ["voting", communityId, votingId] as const;
}

export function useVotings(communityId: string | undefined) {
  return useQuery({
    queryKey: votingsKey(communityId),
    queryFn: () => api.get<Voting[]>(`/communities/${communityId}/votings`),
    enabled: Boolean(communityId),
  });
}

export function useVoting(communityId: string | undefined, votingId: string | undefined) {
  return useQuery({
    queryKey: votingKey(communityId, votingId),
    queryFn: () => api.get<VotingDetail>(`/communities/${communityId}/votings/${votingId}`),
    enabled: Boolean(communityId) && Boolean(votingId),
  });
}

export function useCreateVoting(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      title: string; description?: string; opensAt?: string; closesAt: string; options: string[];
    }) => api.post<Voting>(`/communities/${communityId}/votings`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: votingsKey(communityId) }); },
  });
}

export function useOpenVoting(communityId: string | undefined, votingId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.post<Voting>(`/communities/${communityId}/votings/${votingId}/open`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: votingsKey(communityId) });
      void queryClient.invalidateQueries({ queryKey: votingKey(communityId, votingId) });
    },
  });
}

export function useCloseVoting(communityId: string | undefined, votingId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.post<Voting>(`/communities/${communityId}/votings/${votingId}/close`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: votingsKey(communityId) });
      void queryClient.invalidateQueries({ queryKey: votingKey(communityId, votingId) });
    },
  });
}

export function useCastVote(communityId: string | undefined, votingId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (optionId: string) =>
      api.post<VotingDetail>(`/communities/${communityId}/votings/${votingId}/votes${actingQuery(communityId, "VOTE")}`, { optionId }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: votingsKey(communityId) });
      void queryClient.invalidateQueries({ queryKey: votingKey(communityId, votingId) });
    },
  });
}

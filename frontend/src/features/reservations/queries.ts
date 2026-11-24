import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { CommonArea, Reservation, ReservationStatus } from "../../types/api";

function areasKey(communityId: string | undefined) {
  return ["common-areas", communityId] as const;
}

function reservationsKey(communityId: string | undefined, mine: boolean, areaId?: string) {
  return ["reservations", communityId, mine, areaId ?? "ALL"] as const;
}

export function useCommonAreas(communityId: string | undefined) {
  return useQuery({
    queryKey: areasKey(communityId),
    queryFn: () => api.get<CommonArea[]>(`/communities/${communityId}/common-areas`),
    enabled: Boolean(communityId),
  });
}

export function useCreateCommonArea(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      name: string; description?: string; location?: string; capacity?: number;
      requiresApproval?: boolean; active?: boolean;
    }) => api.post<CommonArea>(`/communities/${communityId}/common-areas`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: areasKey(communityId) }); },
  });
}

export function useUpdateCommonArea(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ areaId, ...payload }: {
      areaId: string; name: string; description?: string; location?: string; capacity?: number;
      requiresApproval?: boolean; active?: boolean;
    }) => api.patch<CommonArea>(`/communities/${communityId}/common-areas/${areaId}`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: areasKey(communityId) }); },
  });
}

export function useReservations(communityId: string | undefined, mine = false, areaId?: string) {
  return useQuery({
    queryKey: reservationsKey(communityId, mine, areaId),
    queryFn: () => {
      const params = new URLSearchParams();
      if (mine) params.set("mine", "true");
      if (areaId) params.set("areaId", areaId);
      const query = params.size ? `?${params.toString()}` : "";
      return api.get<Reservation[]>(`/communities/${communityId}/reservations${query}`);
    },
    enabled: Boolean(communityId),
  });
}

export function useCreateReservation(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      areaId: string; date: string; startTime: string; endTime: string; notes?: string;
    }) => api.post<Reservation>(`/communities/${communityId}/reservations`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: ["reservations", communityId] }); },
  });
}

export function useReviewReservation(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ reservationId, status }: { reservationId: string; status: ReservationStatus }) =>
      api.patch<Reservation>(`/communities/${communityId}/reservations/${reservationId}`, { status }),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: ["reservations", communityId] }); },
  });
}

export function useCancelReservation(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reservationId: string) =>
      api.delete<void>(`/communities/${communityId}/reservations/${reservationId}`),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: ["reservations", communityId] }); },
  });
}

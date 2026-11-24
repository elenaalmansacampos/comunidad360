import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { actingQuery } from "../authorizations/delegationStore";
import { api } from "../../services/apiClient";
import type {
  Incident,
  IncidentAttachment,
  IncidentComment,
  IncidentDetail,
  IncidentPriority,
  IncidentStatus,
} from "../../types/api";

function incidentsKey(communityId: string | undefined) {
  return ["incidents", communityId] as const;
}

function incidentKey(communityId: string | undefined, incidentId: string | undefined) {
  return ["incident", communityId, incidentId] as const;
}

function invalidateIncident(queryClient: ReturnType<typeof useQueryClient>,
                           communityId: string | undefined, incidentId?: string) {
  void queryClient.invalidateQueries({ queryKey: incidentsKey(communityId) });
  if (incidentId) {
    void queryClient.invalidateQueries({ queryKey: incidentKey(communityId, incidentId) });
  }
}

export function useIncidents(communityId: string | undefined, status?: IncidentStatus,
                             priority?: IncidentPriority, assignedToMe = false) {
  return useQuery({
    queryKey: [...incidentsKey(communityId), status ?? "all", priority ?? "all", assignedToMe],
    queryFn: () => {
      const params = new URLSearchParams();
      if (status) params.set("status", status);
      if (priority) params.set("priority", priority);
      if (assignedToMe) params.set("assignedToMe", "true");
      const query = params.size > 0 ? `?${params.toString()}` : "";
      return api.get<Incident[]>(`/communities/${communityId}/incidents${query}`);
    },
    enabled: Boolean(communityId),
  });
}

export function useIncident(communityId: string | undefined, incidentId: string | undefined) {
  return useQuery({
    queryKey: incidentKey(communityId, incidentId),
    queryFn: () => api.get<IncidentDetail>(`/communities/${communityId}/incidents/${incidentId}`),
    enabled: Boolean(communityId) && Boolean(incidentId),
  });
}

export function useCreateIncident(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      title: string;
      description: string;
      category: string;
      priority: IncidentPriority;
      unitId?: string;
    }) => api.post<Incident>(`/communities/${communityId}/incidents${actingQuery(communityId, "INCIDENT")}`, payload),
    onSuccess: () => invalidateIncident(queryClient, communityId),
  });
}

export function useUpdateIncident(communityId: string | undefined, incidentId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      status?: IncidentStatus;
      assignedToUserId?: string;
      version?: number;
    }) => api.patch<Incident>(`/communities/${communityId}/incidents/${incidentId}`, payload),
    onSuccess: () => invalidateIncident(queryClient, communityId, incidentId),
  });
}

export function useAddIncidentComment(communityId: string | undefined, incidentId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { body: string; internal?: boolean }) =>
      api.post<IncidentComment>(`/communities/${communityId}/incidents/${incidentId}/comments${actingQuery(communityId, "INCIDENT")}`, payload),
    onSuccess: () => invalidateIncident(queryClient, communityId, incidentId),
  });
}

export function useUploadIncidentAttachment(communityId: string | undefined,
                                            incidentId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) =>
      api.post<IncidentAttachment>(
        `/communities/${communityId}/incidents/${incidentId}/attachments${actingQuery(communityId, "INCIDENT")}`,
        formData,
      ),
    onSuccess: () => invalidateIncident(queryClient, communityId, incidentId),
  });
}

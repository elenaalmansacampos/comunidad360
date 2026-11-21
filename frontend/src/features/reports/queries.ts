import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { ReportFrequency, ReportSchedule } from "../../types/api";

function schedulesKey(communityId: string | undefined) {
  return ["report-schedules", communityId] as const;
}

export function useReportSchedules(communityId: string | undefined) {
  return useQuery({
    queryKey: schedulesKey(communityId),
    queryFn: () => api.get<ReportSchedule[]>(`/communities/${communityId}/report-schedules`),
    enabled: Boolean(communityId),
  });
}

export function useCreateReportSchedule(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      name: string; frequency: ReportFrequency; recipients: string[]; enabled: boolean;
    }) => api.post<ReportSchedule>(`/communities/${communityId}/report-schedules`, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: schedulesKey(communityId) });
    },
  });
}

export function useUpdateReportSchedule(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ scheduleId, ...payload }: {
      scheduleId: string; name?: string; frequency?: ReportFrequency; recipients?: string[];
      enabled?: boolean; version?: number;
    }) => api.patch<ReportSchedule>(
      `/communities/${communityId}/report-schedules/${scheduleId}`, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: schedulesKey(communityId) });
    },
  });
}

export function useDeleteReportSchedule(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (scheduleId: string) =>
      api.delete<void>(`/communities/${communityId}/report-schedules/${scheduleId}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: schedulesKey(communityId) });
    },
  });
}

export function useRunReportSchedule(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (scheduleId: string) =>
      api.post<ReportSchedule>(`/communities/${communityId}/report-schedules/${scheduleId}/run`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: schedulesKey(communityId) });
    },
  });
}

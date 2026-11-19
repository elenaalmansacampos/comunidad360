import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type {
  AttendanceType,
  Meeting,
  MeetingAgendaItem,
  MeetingAgreement,
  MeetingDetail,
  MeetingStatus,
  MeetingType,
} from "../../types/api";

function meetingsKey(communityId: string | undefined) {
  return ["meetings", communityId] as const;
}

function meetingKey(communityId: string | undefined, meetingId: string | undefined) {
  return ["meeting", communityId, meetingId] as const;
}

function invalidateMeeting(queryClient: ReturnType<typeof useQueryClient>,
                           communityId: string | undefined, meetingId?: string) {
  void queryClient.invalidateQueries({ queryKey: meetingsKey(communityId) });
  if (meetingId) {
    void queryClient.invalidateQueries({ queryKey: meetingKey(communityId, meetingId) });
  }
  void queryClient.invalidateQueries({ queryKey: ["notifications"] });
}

export function useMeetings(communityId: string | undefined) {
  return useQuery({
    queryKey: meetingsKey(communityId),
    queryFn: () => api.get<Meeting[]>(`/communities/${communityId}/meetings`),
    enabled: Boolean(communityId),
  });
}

export function useMeeting(communityId: string | undefined, meetingId: string | undefined) {
  return useQuery({
    queryKey: meetingKey(communityId, meetingId),
    queryFn: () => api.get<MeetingDetail>(`/communities/${communityId}/meetings/${meetingId}`),
    enabled: Boolean(communityId) && Boolean(meetingId),
  });
}

export function useCreateMeeting(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      title: string;
      type: MeetingType;
      scheduledStart: string;
      scheduledEnd?: string;
      location?: string;
      status?: MeetingStatus;
    }) => api.post<Meeting>(`/communities/${communityId}/meetings`, payload),
    onSuccess: () => invalidateMeeting(queryClient, communityId),
  });
}

export function useUpdateMeeting(communityId: string | undefined, meetingId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      title?: string;
      type?: MeetingType;
      scheduledStart?: string;
      scheduledEnd?: string;
      location?: string;
      status?: MeetingStatus;
      version?: number;
    }) => api.patch<Meeting>(`/communities/${communityId}/meetings/${meetingId}`, payload),
    onSuccess: () => invalidateMeeting(queryClient, communityId, meetingId),
  });
}

export function useAddAgendaItem(communityId: string | undefined, meetingId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { title: string; description?: string }) =>
      api.post<MeetingAgendaItem>(
        `/communities/${communityId}/meetings/${meetingId}/agenda`,
        payload,
      ),
    onSuccess: () => invalidateMeeting(queryClient, communityId, meetingId),
  });
}

export function useRecordAttendance(communityId: string | undefined, meetingId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      membershipId: string;
      attendanceType: AttendanceType;
      representedByMembershipId?: string;
    }) =>
      api.post(
        `/communities/${communityId}/meetings/${meetingId}/attendance`,
        payload,
      ),
    onSuccess: () => invalidateMeeting(queryClient, communityId, meetingId),
  });
}

export function useCreateAgreement(communityId: string | undefined, meetingId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      agendaItemId?: string;
      title: string;
      description?: string;
      votesFor: number;
      votesAgainst: number;
      abstentions: number;
    }) =>
      api.post<MeetingAgreement>(
        `/communities/${communityId}/meetings/${meetingId}/agreements`,
        payload,
      ),
    onSuccess: () => invalidateMeeting(queryClient, communityId, meetingId),
  });
}

export function useLinkMinutes(communityId: string | undefined, meetingId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (documentId: string) =>
      api.post<Meeting>(`/communities/${communityId}/meetings/${meetingId}/minutes`, { documentId }),
    onSuccess: () => invalidateMeeting(queryClient, communityId, meetingId),
  });
}

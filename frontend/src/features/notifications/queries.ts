import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { AppNotification, NotificationPreference } from "../../types/api";

function notificationsKey(unreadOnly = false) {
  return ["notifications", unreadOnly] as const;
}

const unreadKey = ["notifications", "unread-count"] as const;
const preferencesKey = ["notification-preferences"] as const;

function invalidateNotifications(queryClient: ReturnType<typeof useQueryClient>) {
  void queryClient.invalidateQueries({ queryKey: ["notifications"] });
}

export function useNotifications(unreadOnly = false) {
  return useQuery({
    queryKey: notificationsKey(unreadOnly),
    queryFn: () => api.get<AppNotification[]>(`/notifications${unreadOnly ? "?unreadOnly=true" : ""}`),
  });
}

export function useUnreadNotificationCount() {
  return useQuery({
    queryKey: unreadKey,
    queryFn: () => api.get<{ count: number }>("/notifications/unread-count"),
    refetchInterval: 30_000,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (notificationId: string) =>
      api.patch<AppNotification>(`/notifications/${notificationId}/read`),
    onSuccess: () => invalidateNotifications(queryClient),
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.post<{ updated: number }>("/notifications/read-all"),
    onSuccess: () => invalidateNotifications(queryClient),
  });
}

export function useNotificationPreferences() {
  return useQuery({
    queryKey: preferencesKey,
    queryFn: () => api.get<NotificationPreference[]>("/me/notification-preferences"),
  });
}

export function useUpdateNotificationPreferences() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (preferences: NotificationPreference[]) =>
      api.put<NotificationPreference[]>("/me/notification-preferences", { preferences }),
    onSuccess: (data) => {
      queryClient.setQueryData(preferencesKey, data);
      void queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

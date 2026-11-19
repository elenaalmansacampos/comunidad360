import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { ChatChannel, ChatChannelType, ChatMessage } from "../../types/api";

function channelsKey(communityId: string | undefined) { return ["chat-channels", communityId] as const; }

export function useChatChannels(communityId: string | undefined) {
  return useQuery({
    queryKey: channelsKey(communityId),
    queryFn: () => api.get<ChatChannel[]>(`/communities/${communityId}/chat/channels`),
    enabled: Boolean(communityId),
  });
}

export function useChatMessages(communityId: string | undefined, channelId: string | undefined) {
  return useQuery({
    queryKey: ["chat-messages", communityId, channelId],
    queryFn: () => api.get<ChatMessage[]>(
      `/communities/${communityId}/chat/channels/${channelId}/messages`),
    enabled: Boolean(communityId) && Boolean(channelId),
    refetchInterval: 5000,
  });
}

export function useCreateChatChannel(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      type: ChatChannelType; name: string; description?: string; memberMembershipIds?: string[];
    }) => api.post<ChatChannel>(`/communities/${communityId}/chat/channels`, payload),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: channelsKey(communityId) }); },
  });
}

export function useSendChatMessage(communityId: string | undefined, channelId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: string) => api.post<ChatMessage>(
      `/communities/${communityId}/chat/channels/${channelId}/messages`, { body }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["chat-messages", communityId, channelId] });
    },
  });
}

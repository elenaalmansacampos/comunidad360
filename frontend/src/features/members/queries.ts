import { useMutation } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { Invitation, Role } from "../../types/api";

export function useInviteMember(communityId: string | undefined) {
  return useMutation({
    mutationFn: (payload: { email: string; role: Role }) =>
      api.post<Invitation>(`/communities/${communityId}/invitations`, payload),
  });
}

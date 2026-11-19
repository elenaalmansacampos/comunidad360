package com.comunidad360.community;

import com.comunidad360.community.dto.CommunityResponse;
import com.comunidad360.community.dto.CommunitySummaryResponse;
import com.comunidad360.community.dto.CreateInvitationRequest;
import com.comunidad360.community.dto.InvitationResponse;
import com.comunidad360.community.dto.MemberResponse;
import com.comunidad360.community.dto.UpdateCommunityRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public List<CommunitySummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return communityService.listForUser(user.id());
    }

    @GetMapping("/{communityId}")
    public CommunityResponse get(@PathVariable UUID communityId,
                                 @AuthenticationPrincipal AuthenticatedUser user) {
        return communityService.get(communityId, user.id());
    }

    @PatchMapping("/{communityId}")
    public CommunityResponse update(@PathVariable UUID communityId,
                                    @AuthenticationPrincipal AuthenticatedUser user,
                                    @Valid @RequestBody UpdateCommunityRequest request,
                                    HttpServletRequest httpRequest) {
        return communityService.update(communityId, user.id(), request, httpRequest);
    }

    @GetMapping("/{communityId}/members")
    public List<MemberResponse> members(@PathVariable UUID communityId,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return communityService.members(communityId, user.id());
    }

    @PostMapping("/{communityId}/invitations")
    public ResponseEntity<InvitationResponse> invite(@PathVariable UUID communityId,
                                                     @AuthenticationPrincipal AuthenticatedUser user,
                                                     @Valid @RequestBody CreateInvitationRequest request,
                                                     HttpServletRequest httpRequest) {
        InvitationResponse response = communityService.invite(
                communityId, user.id(), request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{communityId}/invitations")
    public List<InvitationResponse> invitations(@PathVariable UUID communityId,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        return communityService.invitations(communityId, user.id());
    }
}

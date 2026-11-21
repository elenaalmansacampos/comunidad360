package com.comunidad360.organization;

import com.comunidad360.organization.dto.*;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/mine")
    public List<OrganizationResponse> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return organizationService.mine(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody CreateOrganizationRequest request,
                                       HttpServletRequest httpRequest) {
        return organizationService.create(user.id(), request, httpRequest);
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse get(@PathVariable UUID organizationId,
                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return organizationService.get(organizationId, user.id());
    }

    @PatchMapping("/{organizationId}")
    public OrganizationResponse update(@PathVariable UUID organizationId,
                                       @AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody UpdateOrganizationRequest request,
                                       HttpServletRequest httpRequest) {
        return organizationService.update(organizationId, user.id(), request, httpRequest);
    }

    @GetMapping("/{organizationId}/communities")
    public List<OrganizationCommunityResponse> communities(@PathVariable UUID organizationId,
                                                           @AuthenticationPrincipal AuthenticatedUser user) {
        return organizationService.communities(organizationId, user.id());
    }

    @PostMapping("/{organizationId}/communities/{communityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attachCommunity(@PathVariable UUID organizationId, @PathVariable UUID communityId,
                                @AuthenticationPrincipal AuthenticatedUser user,
                                HttpServletRequest httpRequest) {
        organizationService.attachCommunity(organizationId, communityId, user.id(), httpRequest);
    }

    @DeleteMapping("/{organizationId}/communities/{communityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachCommunity(@PathVariable UUID organizationId, @PathVariable UUID communityId,
                                @AuthenticationPrincipal AuthenticatedUser user,
                                HttpServletRequest httpRequest) {
        organizationService.detachCommunity(organizationId, communityId, user.id(), httpRequest);
    }

    @GetMapping("/{organizationId}/members")
    public List<OrganizationMemberResponse> members(@PathVariable UUID organizationId,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return organizationService.members(organizationId, user.id());
    }

    @PostMapping("/{organizationId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationMemberResponse addMember(@PathVariable UUID organizationId,
                                                @AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody AddOrganizationMemberRequest request,
                                                HttpServletRequest httpRequest) {
        return organizationService.addMember(organizationId, user.id(), request, httpRequest);
    }

    @DeleteMapping("/{organizationId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID organizationId, @PathVariable UUID userId,
                             @AuthenticationPrincipal AuthenticatedUser user,
                             HttpServletRequest httpRequest) {
        organizationService.removeMember(organizationId, userId, user.id(), httpRequest);
    }
}

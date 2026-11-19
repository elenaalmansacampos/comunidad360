package com.comunidad360.votings;

import com.comunidad360.security.AuthenticatedUser;
import com.comunidad360.votings.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/votings")
public class VotingController {
    private final VotingService votingService;
    public VotingController(VotingService votingService) { this.votingService = votingService; }

    @GetMapping
    public List<VotingResponse> list(@PathVariable UUID communityId,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return votingService.list(communityId, user.id());
    }

    @PostMapping
    public ResponseEntity<VotingResponse> create(@PathVariable UUID communityId,
                                                 @AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody CreateVotingRequest request,
                                                 HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(votingService.create(communityId, user.id(), request, httpRequest));
    }

    @GetMapping("/{votingId}")
    public VotingDetailResponse detail(@PathVariable UUID communityId, @PathVariable UUID votingId,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return votingService.detail(communityId, user.id(), votingId);
    }

    @PostMapping("/{votingId}/open")
    public VotingResponse open(@PathVariable UUID communityId, @PathVariable UUID votingId,
                               @AuthenticationPrincipal AuthenticatedUser user,
                               HttpServletRequest httpRequest) {
        return votingService.open(communityId, user.id(), votingId, httpRequest);
    }

    @PostMapping("/{votingId}/close")
    public VotingResponse close(@PathVariable UUID communityId, @PathVariable UUID votingId,
                                @AuthenticationPrincipal AuthenticatedUser user,
                                HttpServletRequest httpRequest) {
        return votingService.close(communityId, user.id(), votingId, httpRequest);
    }

    @PostMapping("/{votingId}/votes")
    public VotingDetailResponse vote(@PathVariable UUID communityId, @PathVariable UUID votingId,
                                     @AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody CastVoteRequest request,
                                     HttpServletRequest httpRequest) {
        return votingService.castVote(communityId, user.id(), votingId, request, httpRequest);
    }

    @GetMapping("/{votingId}/results")
    public VotingResultsResponse results(@PathVariable UUID communityId, @PathVariable UUID votingId,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return votingService.detail(communityId, user.id(), votingId).results();
    }
}

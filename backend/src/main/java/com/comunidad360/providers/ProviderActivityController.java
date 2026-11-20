package com.comunidad360.providers;

import com.comunidad360.providers.dto.*;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/providers/{providerId}")
public class ProviderActivityController {

    private final ProviderActivityService activityService;

    public ProviderActivityController(ProviderActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/contracts")
    public List<ProviderContractResponse> contracts(@PathVariable UUID communityId,
                                                    @PathVariable UUID providerId,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return activityService.contracts(communityId, user.id(), providerId);
    }

    @PostMapping("/contracts")
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderContractResponse createContract(@PathVariable UUID communityId,
                                                   @PathVariable UUID providerId,
                                                   @AuthenticationPrincipal AuthenticatedUser user,
                                                   @Valid @RequestBody CreateProviderContractRequest request,
                                                   HttpServletRequest httpRequest) {
        return activityService.createContract(communityId, user.id(), providerId, request, httpRequest);
    }

    @PatchMapping("/contracts/{contractId}")
    public ProviderContractResponse updateContract(@PathVariable UUID communityId,
                                                   @PathVariable UUID providerId,
                                                   @PathVariable UUID contractId,
                                                   @AuthenticationPrincipal AuthenticatedUser user,
                                                   @Valid @RequestBody UpdateProviderContractRequest request,
                                                   HttpServletRequest httpRequest) {
        return activityService.updateContract(communityId, user.id(), providerId, contractId, request, httpRequest);
    }

    @GetMapping("/budgets")
    public List<ProviderBudgetResponse> budgets(@PathVariable UUID communityId,
                                                @PathVariable UUID providerId,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        return activityService.budgets(communityId, user.id(), providerId);
    }

    @PostMapping("/budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderBudgetResponse createBudget(@PathVariable UUID communityId,
                                               @PathVariable UUID providerId,
                                               @AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CreateProviderBudgetRequest request,
                                               HttpServletRequest httpRequest) {
        return activityService.createBudget(communityId, user.id(), providerId, request, httpRequest);
    }

    @PatchMapping("/budgets/{budgetId}")
    public ProviderBudgetResponse updateBudget(@PathVariable UUID communityId,
                                               @PathVariable UUID providerId,
                                               @PathVariable UUID budgetId,
                                               @AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody UpdateProviderBudgetRequest request,
                                               HttpServletRequest httpRequest) {
        return activityService.updateBudget(communityId, user.id(), providerId, budgetId, request, httpRequest);
    }

    @GetMapping("/notes")
    public List<ProviderNoteResponse> notes(@PathVariable UUID communityId,
                                            @PathVariable UUID providerId,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        return activityService.notes(communityId, user.id(), providerId);
    }

    @PostMapping("/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderNoteResponse addNote(@PathVariable UUID communityId,
                                        @PathVariable UUID providerId,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody CreateProviderNoteRequest request,
                                        HttpServletRequest httpRequest) {
        return activityService.addNote(communityId, user.id(), providerId, request, httpRequest);
    }

    @GetMapping("/history")
    public List<ProviderHistoryEntryResponse> history(@PathVariable UUID communityId,
                                                      @PathVariable UUID providerId,
                                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return activityService.history(communityId, user.id(), providerId);
    }
}

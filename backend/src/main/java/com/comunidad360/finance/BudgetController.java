package com.comunidad360.finance;

import com.comunidad360.finance.dto.BudgetItemRequest;
import com.comunidad360.finance.dto.BudgetResponse;
import com.comunidad360.finance.dto.CreateBudgetRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/finance/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> list(@PathVariable UUID communityId,
                                     @RequestParam(required = false) UUID fiscalYearId,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return budgetService.list(communityId, user.id(), fiscalYearId);
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@PathVariable UUID communityId,
                                                 @AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody CreateBudgetRequest request,
                                                 HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.create(communityId, user.id(), request, httpRequest));
    }

    @PostMapping("/{budgetId}/items")
    public BudgetResponse addItem(@PathVariable UUID communityId,
                                  @PathVariable UUID budgetId,
                                  @AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody BudgetItemRequest request,
                                  HttpServletRequest httpRequest) {
        return budgetService.addItem(communityId, user.id(), budgetId, request, httpRequest);
    }

    @PostMapping("/{budgetId}/approve")
    public BudgetResponse approve(@PathVariable UUID communityId,
                                  @PathVariable UUID budgetId,
                                  @AuthenticationPrincipal AuthenticatedUser user,
                                  HttpServletRequest httpRequest) {
        return budgetService.approve(communityId, user.id(), budgetId, httpRequest);
    }
}

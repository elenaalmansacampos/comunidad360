package com.comunidad360.finance;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.finance.dto.BudgetItemRequest;
import com.comunidad360.finance.dto.BudgetItemResponse;
import com.comunidad360.finance.dto.BudgetResponse;
import com.comunidad360.finance.dto.CreateBudgetRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final FiscalYearService fiscalYearService;
    private final FinanceAccessService financeAccessService;
    private final AuditService auditService;

    public BudgetService(BudgetRepository budgetRepository,
                         BudgetItemRepository budgetItemRepository,
                         FiscalYearService fiscalYearService,
                         FinanceAccessService financeAccessService,
                         AuditService auditService) {
        this.budgetRepository = budgetRepository;
        this.budgetItemRepository = budgetItemRepository;
        this.fiscalYearService = fiscalYearService;
        this.financeAccessService = financeAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(UUID communityId, UUID userId, UUID fiscalYearId) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_VIEW");
        List<Budget> budgets = fiscalYearId == null
                ? budgetRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                : budgetRepository.findByCommunityIdAndFiscalYearIdOrderByCreatedAtDesc(communityId, fiscalYearId);
        return budgets.stream().map(this::toResponse).toList();
    }

    @Transactional
    public BudgetResponse create(UUID communityId, UUID userId, CreateBudgetRequest request,
                                 HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        fiscalYearService.requireOpen(communityId, request.fiscalYearId());
        Instant now = Instant.now();
        Budget budget = new Budget(
                UUID.randomUUID(),
                communityId,
                request.fiscalYearId(),
                BudgetStatus.DRAFT,
                request.notes(),
                now,
                now
        );
        Budget saved = budgetRepository.saveAndFlush(budget);
        BudgetResponse response = toResponse(saved);
        auditService.record(communityId, userId, "USER", "BUDGET_CREATED", "BUDGET",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public BudgetResponse addItem(UUID communityId, UUID userId, UUID budgetId, BudgetItemRequest request,
                                  HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        Budget budget = find(communityId, budgetId);
        if (budget.getStatus() != BudgetStatus.DRAFT) {
            throw BusinessException.conflict("BUDGET_NOT_EDITABLE", "errors.budget.notEditable");
        }
        List<BudgetItem> existing = budgetItemRepository.findByBudgetIdOrderByPositionAsc(budgetId);
        BudgetItem item = new BudgetItem(
                UUID.randomUUID(),
                communityId,
                budgetId,
                request.type(),
                request.category().trim(),
                request.description().trim(),
                request.plannedAmount(),
                existing.size()
        );
        budgetItemRepository.save(item);
        BudgetResponse response = recalculate(budget);
        auditService.record(communityId, userId, "USER", "BUDGET_ITEM_CREATED", "BUDGET",
                budgetId, null, BudgetItemResponse.from(item), httpRequest);
        return response;
    }

    @Transactional
    public BudgetResponse approve(UUID communityId, UUID userId, UUID budgetId,
                                  HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        Budget budget = find(communityId, budgetId);
        if (budget.getStatus() == BudgetStatus.DRAFT) {
            BudgetResponse before = toResponse(budget);
            budget.setStatus(BudgetStatus.APPROVED);
            budget.setApprovedAt(Instant.now());
            budget.setApprovedByUserId(userId);
            budget.setUpdatedAt(Instant.now());
            Budget saved = budgetRepository.saveAndFlush(budget);
            BudgetResponse after = toResponse(saved);
            auditService.record(communityId, userId, "USER", "BUDGET_APPROVED", "BUDGET",
                    budgetId, before, after, httpRequest);
            return after;
        }
        return toResponse(budget);
    }

    private Budget find(UUID communityId, UUID budgetId) {
        return budgetRepository.findByIdAndCommunityId(budgetId, communityId)
                .orElseThrow(() -> BusinessException.notFound("BUDGET_NOT_FOUND", "errors.budget.notFound"));
    }

    private BudgetResponse recalculate(Budget budget) {
        List<BudgetItem> items = budgetItemRepository.findByBudgetIdOrderByPositionAsc(budget.getId());
        BigDecimal income = items.stream()
                .filter(item -> item.getType() == BudgetItemType.INCOME)
                .map(BudgetItem::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = items.stream()
                .filter(item -> item.getType() == BudgetItemType.EXPENSE)
                .map(BudgetItem::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        budget.setTotalIncome(income);
        budget.setTotalExpense(expense);
        budget.setUpdatedAt(Instant.now());
        Budget saved = budgetRepository.saveAndFlush(budget);
        return toResponse(saved);
    }

    private BudgetResponse toResponse(Budget budget) {
        List<BudgetItemResponse> items = budgetItemRepository.findByBudgetIdOrderByPositionAsc(budget.getId()).stream()
                .map(BudgetItemResponse::from)
                .toList();
        return BudgetResponse.from(budget, items);
    }
}

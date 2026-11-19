package com.comunidad360.finance;

import com.comunidad360.finance.dto.CategoryReportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FinanceReportService {

    private final FiscalYearService fiscalYearService;
    private final FinanceAccessService financeAccessService;
    private final LedgerEntryRepository ledgerEntryRepository;

    public FinanceReportService(FiscalYearService fiscalYearService,
                                FinanceAccessService financeAccessService,
                                LedgerEntryRepository ledgerEntryRepository) {
        this.fiscalYearService = fiscalYearService;
        this.financeAccessService = financeAccessService;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryReportResponse> categorySummary(UUID communityId, UUID userId, UUID fiscalYearId) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_VIEW");
        FiscalYear fiscalYear = fiscalYearId == null
                ? fiscalYearService.current(communityId)
                : fiscalYearService.find(communityId, fiscalYearId);
        List<LedgerEntry> entries = ledgerEntryRepository
                .findByCommunityIdAndFiscalYearIdAndStatusOrderByCategoryAsc(
                        communityId, fiscalYear.getId(), LedgerEntryStatus.POSTED);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (LedgerEntry entry : entries) {
            String key = entry.getType().name() + "|" + entry.getCategory();
            totals.merge(key, entry.getAmount(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    return new CategoryReportResponse(parts[0], parts[1], entry.getValue());
                })
                .toList();
    }
}

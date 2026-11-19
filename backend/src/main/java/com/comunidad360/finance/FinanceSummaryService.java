package com.comunidad360.finance;

import com.comunidad360.finance.dto.FinanceSummaryResponse;
import com.comunidad360.membership.Membership;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FinanceSummaryService {

    private final FiscalYearService fiscalYearService;
    private final FinanceAccessService financeAccessService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AssessmentChargeRepository chargeRepository;
    private final InvoiceRepository invoiceRepository;

    public FinanceSummaryService(FiscalYearService fiscalYearService,
                                 FinanceAccessService financeAccessService,
                                 LedgerEntryRepository ledgerEntryRepository,
                                 AssessmentChargeRepository chargeRepository,
                                 InvoiceRepository invoiceRepository) {
        this.fiscalYearService = fiscalYearService;
        this.financeAccessService = financeAccessService;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.chargeRepository = chargeRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public FinanceSummaryResponse summary(UUID communityId, UUID userId, UUID fiscalYearId) {
        Membership membership = financeAccessService.requireFinanceView(communityId, userId);
        FiscalYear fiscalYear = fiscalYearId == null
                ? fiscalYearService.current(communityId)
                : fiscalYearService.find(communityId, fiscalYearId);

        if (financeAccessService.isStaff(membership)) {
            BigDecimal income = ledgerEntryRepository.sumByType(
                    communityId, fiscalYear.getId(), LedgerEntryStatus.POSTED, LedgerEntryType.INCOME);
            BigDecimal expense = ledgerEntryRepository.sumByType(
                    communityId, fiscalYear.getId(), LedgerEntryStatus.POSTED, LedgerEntryType.EXPENSE);
            BigDecimal pending = chargeRepository.sumPending(
                    communityId,
                    fiscalYear.getId(),
                    List.of(ChargeStatus.PENDING, ChargeStatus.PARTIAL)
            );
            BigDecimal invoicesPending = invoiceRepository.sumPending(
                    communityId, fiscalYear.getId(), InvoiceStatus.REGISTERED);
            return new FinanceSummaryResponse(
                    fiscalYear.getId(),
                    fiscalYear.getYear(),
                    "COMMUNITY",
                    "EUR",
                    income.subtract(expense),
                    income,
                    expense,
                    pending,
                    invoicesPending
            );
        }

        Set<UUID> unitIds = financeAccessService.ownedUnitIds(communityId, userId);
        BigDecimal pending = unitIds.isEmpty()
                ? BigDecimal.ZERO
                : chargeRepository.sumPendingForUnits(
                        communityId,
                        fiscalYear.getId(),
                        unitIds,
                        List.of(ChargeStatus.PENDING, ChargeStatus.PARTIAL)
                );
        return new FinanceSummaryResponse(
                fiscalYear.getId(),
                fiscalYear.getYear(),
                "OWN",
                "EUR",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                pending,
                BigDecimal.ZERO
        );
    }
}

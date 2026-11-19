package com.comunidad360.finance;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.finance.dto.CreateFiscalYearRequest;
import com.comunidad360.finance.dto.FiscalYearResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FiscalYearService {

    private final FiscalYearRepository fiscalYearRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final FinanceAccessService financeAccessService;
    private final AuditService auditService;

    public FiscalYearService(FiscalYearRepository fiscalYearRepository,
                             LedgerEntryRepository ledgerEntryRepository,
                             FinanceAccessService financeAccessService,
                             AuditService auditService) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.financeAccessService = financeAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<FiscalYearResponse> list(UUID communityId, UUID userId) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_VIEW");
        return fiscalYearRepository.findByCommunityIdOrderByYearDesc(communityId).stream()
                .map(FiscalYearResponse::from)
                .toList();
    }

    @Transactional
    public FiscalYearResponse create(UUID communityId, UUID userId, CreateFiscalYearRequest request,
                                     HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        if (request.endDate().isBefore(request.startDate())) {
            throw BusinessException.unprocessable(
                    "FISCAL_YEAR_DATES_INVALID", "errors.fiscalYear.invalidDates");
        }
        if (fiscalYearRepository.existsByCommunityIdAndYear(communityId, request.year())) {
            throw BusinessException.conflict("FISCAL_YEAR_EXISTS", "errors.fiscalYear.exists");
        }
        FiscalYear saved = fiscalYearRepository.saveAndFlush(new FiscalYear(
                UUID.randomUUID(),
                communityId,
                request.year(),
                request.startDate(),
                request.endDate(),
                FiscalYearStatus.OPEN,
                Instant.now()
        ));
        FiscalYearResponse response = FiscalYearResponse.from(saved);
        auditService.record(communityId, userId, "USER", "FISCAL_YEAR_CREATED", "FISCAL_YEAR",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public FiscalYearResponse close(UUID communityId, UUID userId, UUID fiscalYearId,
                                    HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        FiscalYear fiscalYear = find(communityId, fiscalYearId);
        if (fiscalYear.getStatus() == FiscalYearStatus.CLOSED) {
            return FiscalYearResponse.from(fiscalYear);
        }
        long drafts = ledgerEntryRepository.countByCommunityIdAndFiscalYearIdAndStatus(
                communityId, fiscalYearId, LedgerEntryStatus.DRAFT);
        if (drafts > 0) {
            throw BusinessException.unprocessable(
                    "FISCAL_YEAR_HAS_DRAFT_ENTRIES", "errors.fiscalYear.hasDraftEntries");
        }
        FiscalYearResponse before = FiscalYearResponse.from(fiscalYear);
        fiscalYear.setStatus(FiscalYearStatus.CLOSED);
        fiscalYear.setClosedAt(Instant.now());
        fiscalYear.setClosedByUserId(userId);
        FiscalYear saved = fiscalYearRepository.saveAndFlush(fiscalYear);
        FiscalYearResponse after = FiscalYearResponse.from(saved);
        auditService.record(communityId, userId, "USER", "FISCAL_YEAR_CLOSED", "FISCAL_YEAR",
                fiscalYearId, before, after, httpRequest);
        return after;
    }

    @Transactional(readOnly = true)
    public FiscalYear requireOpen(UUID communityId, UUID fiscalYearId) {
        FiscalYear fiscalYear = find(communityId, fiscalYearId);
        if (fiscalYear.getStatus() != FiscalYearStatus.OPEN) {
            throw BusinessException.unprocessable("FISCAL_YEAR_CLOSED", "errors.fiscalYear.closed");
        }
        return fiscalYear;
    }

    @Transactional(readOnly = true)
    public FiscalYear current(UUID communityId) {
        return fiscalYearRepository.findFirstByCommunityIdAndStatusOrderByYearDesc(
                        communityId, FiscalYearStatus.OPEN)
                .or(() -> fiscalYearRepository.findFirstByCommunityIdOrderByYearDesc(communityId))
                .orElseThrow(() -> BusinessException.notFound(
                        "FISCAL_YEAR_NOT_FOUND", "errors.fiscalYear.notFound"));
    }

    @Transactional(readOnly = true)
    public FiscalYear find(UUID communityId, UUID fiscalYearId) {
        return fiscalYearRepository.findByIdAndCommunityId(fiscalYearId, communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "FISCAL_YEAR_NOT_FOUND", "errors.fiscalYear.notFound"));
    }
}

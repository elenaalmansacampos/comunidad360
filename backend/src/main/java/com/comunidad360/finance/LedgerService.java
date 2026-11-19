package com.comunidad360.finance;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.finance.dto.CreateLedgerEntryRequest;
import com.comunidad360.finance.dto.LedgerEntryResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final FiscalYearService fiscalYearService;
    private final FinanceAccessService financeAccessService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository,
                         FiscalYearService fiscalYearService,
                         FinanceAccessService financeAccessService,
                         IdempotencyService idempotencyService,
                         AuditService auditService) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.fiscalYearService = fiscalYearService;
        this.financeAccessService = financeAccessService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> list(UUID communityId, UUID userId, UUID fiscalYearId) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_VIEW");
        List<LedgerEntry> entries = fiscalYearId == null
                ? ledgerEntryRepository.findByCommunityIdOrderByEntryDateDesc(communityId)
                : ledgerEntryRepository.findByCommunityIdAndFiscalYearIdOrderByEntryDateDesc(
                        communityId, fiscalYearId);
        return entries.stream().map(LedgerEntryResponse::from).toList();
    }

    @Transactional
    public LedgerEntryResponse create(UUID communityId, UUID userId, CreateLedgerEntryRequest request,
                                      String idempotencyKey, HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        Optional<UUID> existing = idempotencyService.findExisting(
                communityId, userId, "CREATE_LEDGER_ENTRY", idempotencyKey, request);
        if (existing.isPresent()) {
            return LedgerEntryResponse.from(find(communityId, existing.get()));
        }
        if (request.type() != LedgerEntryType.INCOME && request.type() != LedgerEntryType.EXPENSE) {
            throw BusinessException.unprocessable("LEDGER_TYPE_INVALID", "errors.ledger.typeInvalid");
        }
        fiscalYearService.requireOpen(communityId, request.fiscalYearId());
        Long nextNumber = ledgerEntryRepository.maxEntryNumber(communityId, request.fiscalYearId()) + 1;
        Instant now = Instant.now();
        LedgerEntry entry = new LedgerEntry(
                UUID.randomUUID(),
                communityId,
                request.fiscalYearId(),
                nextNumber,
                request.entryDate(),
                request.type(),
                request.category().trim(),
                request.description().trim(),
                request.amount(),
                LedgerEntryStatus.DRAFT,
                null,
                userId,
                now,
                now
        );
        LedgerEntry saved = ledgerEntryRepository.saveAndFlush(entry);
        LedgerEntryResponse response = LedgerEntryResponse.from(saved);
        idempotencyService.record(communityId, userId, "CREATE_LEDGER_ENTRY", idempotencyKey,
                request, saved.getId());
        auditService.record(communityId, userId, "USER", "LEDGER_ENTRY_CREATED", "LEDGER_ENTRY",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public LedgerEntryResponse post(UUID communityId, UUID userId, UUID entryId,
                                    HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        LedgerEntry entry = find(communityId, entryId);
        if (entry.getStatus() != LedgerEntryStatus.DRAFT) {
            throw BusinessException.conflict("LEDGER_NOT_DRAFT", "errors.ledger.notDraft");
        }
        fiscalYearService.requireOpen(communityId, entry.getFiscalYearId());
        LedgerEntryResponse before = LedgerEntryResponse.from(entry);
        entry.setStatus(LedgerEntryStatus.POSTED);
        entry.setPostedAt(Instant.now());
        entry.setUpdatedAt(Instant.now());
        LedgerEntry saved = ledgerEntryRepository.saveAndFlush(entry);
        LedgerEntryResponse after = LedgerEntryResponse.from(saved);
        auditService.record(communityId, userId, "USER", "LEDGER_ENTRY_POSTED", "LEDGER_ENTRY",
                entryId, before, after, httpRequest);
        return after;
    }

    @Transactional
    public LedgerEntryResponse reverse(UUID communityId, UUID userId, UUID entryId,
                                       HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        LedgerEntry original = find(communityId, entryId);
        if (original.getStatus() != LedgerEntryStatus.POSTED) {
            throw BusinessException.unprocessable("LEDGER_NOT_POSTED", "errors.ledger.notPosted");
        }
        fiscalYearService.requireOpen(communityId, original.getFiscalYearId());
        LedgerEntryType reversalType = switch (original.getType()) {
            case INCOME -> LedgerEntryType.EXPENSE;
            case EXPENSE -> LedgerEntryType.INCOME;
            default -> throw BusinessException.unprocessable(
                    "LEDGER_TYPE_INVALID", "errors.ledger.typeInvalid");
        };
        Long nextNumber = ledgerEntryRepository.maxEntryNumber(communityId, original.getFiscalYearId()) + 1;
        Instant now = Instant.now();
        LedgerEntry reversal = new LedgerEntry(
                UUID.randomUUID(),
                communityId,
                original.getFiscalYearId(),
                nextNumber,
                LocalDate.now(),
                reversalType,
                original.getCategory(),
                "Reversal: " + original.getDescription(),
                original.getAmount(),
                LedgerEntryStatus.POSTED,
                original.getId(),
                userId,
                now,
                now
        );
        reversal.setPostedAt(now);
        LedgerEntry saved = ledgerEntryRepository.saveAndFlush(reversal);
        LedgerEntryResponse response = LedgerEntryResponse.from(saved);
        auditService.record(communityId, userId, "USER", "LEDGER_ENTRY_REVERSED", "LEDGER_ENTRY",
                entryId, null, response, httpRequest);
        return response;
    }

    private LedgerEntry find(UUID communityId, UUID entryId) {
        return ledgerEntryRepository.findByIdAndCommunityId(entryId, communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "LEDGER_NOT_FOUND", "errors.ledger.notFound"));
    }
}

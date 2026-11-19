package com.comunidad360.finance;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.finance.dto.CreateInvoiceRequest;
import com.comunidad360.finance.dto.InvoiceResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final FiscalYearService fiscalYearService;
    private final FinanceAccessService financeAccessService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          FiscalYearService fiscalYearService,
                          FinanceAccessService financeAccessService,
                          IdempotencyService idempotencyService,
                          AuditService auditService) {
        this.invoiceRepository = invoiceRepository;
        this.fiscalYearService = fiscalYearService;
        this.financeAccessService = financeAccessService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> list(UUID communityId, UUID userId, UUID fiscalYearId) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_VIEW");
        List<Invoice> invoices = fiscalYearId == null
                ? invoiceRepository.findByCommunityIdOrderByIssueDateDesc(communityId)
                : invoiceRepository.findByCommunityIdAndFiscalYearIdOrderByIssueDateDesc(
                        communityId, fiscalYearId);
        return invoices.stream().map(InvoiceResponse::from).toList();
    }

    @Transactional
    public InvoiceResponse create(UUID communityId, UUID userId, CreateInvoiceRequest request,
                                  String idempotencyKey, HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        Optional<UUID> existing = idempotencyService.findExisting(
                communityId, userId, "CREATE_INVOICE", idempotencyKey, request);
        if (existing.isPresent()) {
            return InvoiceResponse.from(find(communityId, existing.get()));
        }
        fiscalYearService.requireOpen(communityId, request.fiscalYearId());
        if (request.subtotal().add(request.taxAmount()).compareTo(request.total()) != 0) {
            throw BusinessException.unprocessable(
                    "INVOICE_AMOUNT_INVALID", "errors.invoice.amountInvalid");
        }
        if (invoiceRepository.existsByCommunityIdAndProviderNameIgnoreCaseAndNumber(
                communityId, request.providerName().trim(), request.number().trim())) {
            throw BusinessException.conflict("INVOICE_DUPLICATE", "errors.invoice.duplicate");
        }
        Instant now = Instant.now();
        Invoice invoice = new Invoice(
                UUID.randomUUID(),
                communityId,
                request.fiscalYearId(),
                request.providerName().trim(),
                request.number().trim(),
                request.issueDate(),
                request.dueDate(),
                request.subtotal(),
                request.taxAmount(),
                request.total(),
                InvoiceStatus.REGISTERED,
                userId,
                now,
                now
        );
        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        InvoiceResponse response = InvoiceResponse.from(saved);
        idempotencyService.record(communityId, userId, "CREATE_INVOICE", idempotencyKey,
                request, saved.getId());
        auditService.record(communityId, userId, "USER", "INVOICE_CREATED", "INVOICE",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public Invoice find(UUID communityId, UUID invoiceId) {
        return invoiceRepository.findByIdAndCommunityId(invoiceId, communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "INVOICE_NOT_FOUND", "errors.invoice.notFound"));
    }
}

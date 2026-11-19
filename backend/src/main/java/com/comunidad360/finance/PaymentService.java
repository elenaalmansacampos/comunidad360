package com.comunidad360.finance;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.finance.dto.CreatePaymentRequest;
import com.comunidad360.finance.dto.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AssessmentChargeRepository chargeRepository;
    private final InvoiceRepository invoiceRepository;
    private final AssessmentRepository assessmentRepository;
    private final FiscalYearService fiscalYearService;
    private final FinanceAccessService financeAccessService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;

    public PaymentService(PaymentRepository paymentRepository,
                          AssessmentChargeRepository chargeRepository,
                          InvoiceRepository invoiceRepository,
                          AssessmentRepository assessmentRepository,
                          FiscalYearService fiscalYearService,
                          FinanceAccessService financeAccessService,
                          IdempotencyService idempotencyService,
                          AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.chargeRepository = chargeRepository;
        this.invoiceRepository = invoiceRepository;
        this.assessmentRepository = assessmentRepository;
        this.fiscalYearService = fiscalYearService;
        this.financeAccessService = financeAccessService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(UUID communityId, UUID userId) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_VIEW");
        return paymentRepository.findByCommunityIdOrderByPaymentDateDesc(communityId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public PaymentResponse create(UUID communityId, UUID userId, CreatePaymentRequest request,
                                  String idempotencyKey, HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        Optional<UUID> existing = idempotencyService.findExisting(
                communityId, userId, "CREATE_PAYMENT", idempotencyKey, request);
        if (existing.isPresent()) {
            return paymentRepository.findByIdAndCommunityId(existing.get(), communityId)
                    .map(PaymentResponse::from)
                    .orElseThrow(() -> BusinessException.notFound(
                            "PAYMENT_NOT_FOUND", "errors.resource.notFound"));
        }

        boolean hasCharge = request.assessmentChargeId() != null;
        boolean hasInvoice = request.invoiceId() != null;
        if (hasCharge == hasInvoice) {
            throw BusinessException.unprocessable(
                    "PAYMENT_TARGET_REQUIRED", "errors.payment.targetRequired");
        }

        BigDecimal amount = request.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.unprocessable(
                    "PAYMENT_AMOUNT_INVALID", "errors.validation");
        }

        UUID unitId = null;
        if (hasCharge) {
            AssessmentCharge charge = chargeRepository.findForUpdate(
                            request.assessmentChargeId(), communityId)
                    .orElseThrow(() -> BusinessException.notFound(
                            "CHARGE_NOT_FOUND", "errors.charge.notFound"));
            if (charge.getStatus() == ChargeStatus.CANCELLED) {
                throw BusinessException.unprocessable(
                        "CHARGE_CANCELLED", "errors.charge.cancelled");
            }
            Assessment assessment = assessmentRepository.findByIdAndCommunityId(
                            charge.getAssessmentId(), communityId)
                    .orElseThrow(() -> BusinessException.notFound(
                            "ASSESSMENT_NOT_FOUND", "errors.assessment.notFound"));
            fiscalYearService.requireOpen(communityId, assessment.getFiscalYearId());
            BigDecimal remaining = charge.getAmount().subtract(charge.getPaidAmount());
            if (amount.compareTo(remaining) > 0) {
                throw BusinessException.unprocessable(
                        "PAYMENT_EXCEEDS", "errors.payment.exceeds");
            }
            BigDecimal paid = charge.getPaidAmount().add(amount);
            charge.setPaidAmount(paid);
            charge.setStatus(paid.compareTo(charge.getAmount()) >= 0 ? ChargeStatus.PAID : ChargeStatus.PARTIAL);
            charge.setUpdatedAt(Instant.now());
            chargeRepository.save(charge);
            unitId = charge.getUnitId();
        } else {
            Invoice invoice = invoiceRepository.findForUpdate(request.invoiceId(), communityId)
                    .orElseThrow(() -> BusinessException.notFound(
                            "INVOICE_NOT_FOUND", "errors.invoice.notFound"));
            if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                throw BusinessException.unprocessable(
                        "INVOICE_NOT_PAYABLE", "errors.invoice.notPayable");
            }
            fiscalYearService.requireOpen(communityId, invoice.getFiscalYearId());
            BigDecimal remaining = invoice.getTotal().subtract(invoice.getPaidAmount());
            if (amount.compareTo(remaining) > 0) {
                throw BusinessException.unprocessable(
                        "PAYMENT_EXCEEDS", "errors.payment.exceeds");
            }
            BigDecimal paid = invoice.getPaidAmount().add(amount);
            invoice.setPaidAmount(paid);
            invoice.setStatus(paid.compareTo(invoice.getTotal()) >= 0 ? InvoiceStatus.PAID : InvoiceStatus.REGISTERED);
            invoice.setUpdatedAt(Instant.now());
            invoiceRepository.save(invoice);
        }

        Instant now = Instant.now();
        Payment payment = new Payment(
                UUID.randomUUID(),
                communityId,
                unitId,
                request.assessmentChargeId(),
                request.invoiceId(),
                amount,
                request.paymentDate(),
                request.method(),
                request.reference(),
                userId,
                now,
                now
        );
        Payment saved = paymentRepository.saveAndFlush(payment);
        PaymentResponse response = PaymentResponse.from(saved);
        idempotencyService.record(communityId, userId, "CREATE_PAYMENT", idempotencyKey,
                request, saved.getId());
        auditService.record(communityId, userId, "USER", "PAYMENT_CREATED", "PAYMENT",
                saved.getId(), null, response, httpRequest);
        return response;
    }
}

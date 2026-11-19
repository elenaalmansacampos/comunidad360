package com.comunidad360.finance;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.config.AppProperties;
import com.comunidad360.finance.dto.AssessmentChargeResponse;
import com.comunidad360.finance.dto.AssessmentResponse;
import com.comunidad360.finance.dto.CreateAssessmentRequest;
import com.comunidad360.membership.Membership;
import com.comunidad360.property.Unit;
import com.comunidad360.property.UnitRepository;
import com.comunidad360.property.UnitStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentChargeRepository chargeRepository;
    private final UnitRepository unitRepository;
    private final FiscalYearService fiscalYearService;
    private final FinanceAccessService financeAccessService;
    private final AllocationService allocationService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final AppProperties properties;

    public AssessmentService(AssessmentRepository assessmentRepository,
                             AssessmentChargeRepository chargeRepository,
                             UnitRepository unitRepository,
                             FiscalYearService fiscalYearService,
                             FinanceAccessService financeAccessService,
                             AllocationService allocationService,
                             IdempotencyService idempotencyService,
                             AuditService auditService,
                             AppProperties properties) {
        this.assessmentRepository = assessmentRepository;
        this.chargeRepository = chargeRepository;
        this.unitRepository = unitRepository;
        this.fiscalYearService = fiscalYearService;
        this.financeAccessService = financeAccessService;
        this.allocationService = allocationService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list(UUID communityId, UUID userId, UUID fiscalYearId) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_VIEW");
        List<Assessment> assessments = fiscalYearId == null
                ? assessmentRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                : assessmentRepository.findByCommunityIdAndFiscalYearIdOrderByCreatedAtDesc(
                        communityId, fiscalYearId);
        return assessments.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AssessmentResponse create(UUID communityId, UUID userId, CreateAssessmentRequest request,
                                     String idempotencyKey, HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        Optional<UUID> existing = idempotencyService.findExisting(
                communityId, userId, "CREATE_ASSESSMENT", idempotencyKey, request);
        if (existing.isPresent()) {
            return toResponse(find(communityId, existing.get()));
        }
        fiscalYearService.requireOpen(communityId, request.fiscalYearId());
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw BusinessException.unprocessable("ASSESSMENT_PERIOD_INVALID", "errors.validation");
        }
        Instant now = Instant.now();
        Assessment assessment = new Assessment(
                UUID.randomUUID(),
                communityId,
                request.fiscalYearId(),
                request.concept().trim(),
                request.type(),
                request.periodStart(),
                request.periodEnd(),
                request.dueDate(),
                request.totalAmount(),
                request.allocationMethod(),
                AssessmentStatus.DRAFT,
                userId,
                now,
                now
        );
        Assessment saved = assessmentRepository.saveAndFlush(assessment);
        AssessmentResponse response = toResponse(saved);
        idempotencyService.record(communityId, userId, "CREATE_ASSESSMENT", idempotencyKey,
                request, saved.getId());
        auditService.record(communityId, userId, "USER", "ASSESSMENT_CREATED", "ASSESSMENT",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public AssessmentResponse issue(UUID communityId, UUID userId, UUID assessmentId,
                                    HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        Assessment assessment = find(communityId, assessmentId);
        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw BusinessException.conflict(
                    "ASSESSMENT_ALREADY_ISSUED", "errors.assessment.alreadyIssued");
        }
        fiscalYearService.requireOpen(communityId, assessment.getFiscalYearId());
        List<Unit> units = unitRepository.findByCommunityIdAndArchivedAtIsNullOrderByCodeAsc(communityId).stream()
                .filter(unit -> unit.getStatus() == UnitStatus.ACTIVE)
                .toList();
        if (units.isEmpty()) {
            throw BusinessException.unprocessable(
                    "NO_ACTIVE_UNITS", "errors.assessment.noActiveUnits");
        }
        List<AllocationService.Allocation> allocations = allocationService.allocate(
                assessment.getTotalAmount(),
                units,
                assessment.getAllocationMethod(),
                properties.getStructure().getCoefficientTolerance()
        );
        Instant now = Instant.now();
        List<AssessmentCharge> charges = allocations.stream()
                .map(allocation -> new AssessmentCharge(
                        UUID.randomUUID(),
                        communityId,
                        assessmentId,
                        allocation.unitId(),
                        allocation.amount(),
                        allocation.coefficientSnapshot(),
                        now,
                        now
                ))
                .toList();
        chargeRepository.saveAll(charges);
        assessment.setStatus(AssessmentStatus.ISSUED);
        assessment.setIssuedAt(now);
        assessment.setUpdatedAt(now);
        Assessment saved = assessmentRepository.saveAndFlush(assessment);
        AssessmentResponse response = toResponse(saved);
        auditService.record(communityId, userId, "USER", "ASSESSMENT_ISSUED", "ASSESSMENT",
                assessmentId, null, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public List<AssessmentChargeResponse> listCharges(UUID communityId, UUID userId, UUID fiscalYearId) {
        Membership membership = financeAccessService.requireFinanceView(communityId, userId);
        FiscalYear fiscalYear = fiscalYearId == null
                ? fiscalYearService.current(communityId)
                : fiscalYearService.find(communityId, fiscalYearId);
        List<AssessmentCharge> charges;
        if (financeAccessService.isStaff(membership)) {
            charges = chargeRepository.findForFiscalYear(communityId, fiscalYear.getId());
        } else {
            Set<UUID> unitIds = financeAccessService.ownedUnitIds(communityId, userId);
            if (unitIds.isEmpty()) {
                return List.of();
            }
            charges = chargeRepository.findForFiscalYearAndUnits(communityId, fiscalYear.getId(), unitIds);
        }
        if (charges.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> unitCodes = unitRepository.findAllById(
                        charges.stream().map(AssessmentCharge::getUnitId).distinct().toList()).stream()
                .collect(Collectors.toMap(Unit::getId, Unit::getCode, (left, right) -> left));
        Map<UUID, LocalDate> dueDates = assessmentRepository.findAllById(
                        charges.stream().map(AssessmentCharge::getAssessmentId).distinct().toList()).stream()
                .collect(Collectors.toMap(Assessment::getId, Assessment::getDueDate, (left, right) -> left));
        return charges.stream()
                .map(charge -> AssessmentChargeResponse.from(
                        charge,
                        unitCodes.get(charge.getUnitId()),
                        dueDates.get(charge.getAssessmentId())))
                .toList();
    }

    private Assessment find(UUID communityId, UUID assessmentId) {
        return assessmentRepository.findByIdAndCommunityId(assessmentId, communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "ASSESSMENT_NOT_FOUND", "errors.assessment.notFound"));
    }

    private AssessmentResponse toResponse(Assessment assessment) {
        return AssessmentResponse.from(assessment, chargeRepository.countByAssessmentId(assessment.getId()));
    }
}

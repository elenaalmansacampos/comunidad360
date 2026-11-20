package com.comunidad360.providers;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.providers.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ProviderActivityService {

    private final ProviderRepository providerRepository;
    private final ProviderContractRepository contractRepository;
    private final ProviderBudgetRepository budgetRepository;
    private final ProviderNoteRepository noteRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public ProviderActivityService(ProviderRepository providerRepository,
                                   ProviderContractRepository contractRepository,
                                   ProviderBudgetRepository budgetRepository,
                                   ProviderNoteRepository noteRepository,
                                   TenantAccessService tenantAccessService,
                                   PermissionService permissionService,
                                   AuditService auditService) {
        this.providerRepository = providerRepository;
        this.contractRepository = contractRepository;
        this.budgetRepository = budgetRepository;
        this.noteRepository = noteRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ProviderContractResponse> contracts(UUID communityId, UUID userId, UUID providerId) {
        requireView(communityId, userId, providerId);
        return contractRepository.findByCommunityIdAndProviderIdOrderByCreatedAtDesc(communityId, providerId)
                .stream().map(ProviderContractResponse::from).toList();
    }

    @Transactional
    public ProviderContractResponse createContract(UUID communityId, UUID userId, UUID providerId,
                                                  CreateProviderContractRequest request,
                                                  HttpServletRequest httpRequest) {
        requireManage(communityId, userId, providerId);
        validateDates(request.startDate(), request.endDate());
        Instant now = Instant.now();
        ProviderContractStatus status = request.status() == null
                ? ProviderContractStatus.DRAFT : request.status();
        ProviderContract contract = contractRepository.saveAndFlush(new ProviderContract(
                UUID.randomUUID(), communityId, providerId, request.title().trim(), clean(request.description()),
                request.startDate(), request.endDate(), request.amount(), status, userId, now, now));
        ProviderContractResponse response = ProviderContractResponse.from(contract);
        auditService.record(communityId, userId, "USER", "PROVIDER_CONTRACT_CREATED", "PROVIDER",
                providerId, null, response, httpRequest);
        return response;
    }

    @Transactional
    public ProviderContractResponse updateContract(UUID communityId, UUID userId, UUID providerId,
                                                  UUID contractId, UpdateProviderContractRequest request,
                                                  HttpServletRequest httpRequest) {
        requireManage(communityId, userId, providerId);
        ProviderContract contract = contractRepository
                .findByIdAndCommunityIdAndProviderId(contractId, communityId, providerId)
                .orElseThrow(() -> BusinessException.notFound("PROVIDER_CONTRACT_NOT_FOUND",
                        "errors.resource.notFound"));
        if (request.version() != null && request.version() != contract.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        ProviderContractResponse before = ProviderContractResponse.from(contract);
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            contract.setTitle(request.title().trim());
        }
        if (request.description() != null) contract.setDescription(clean(request.description()));
        if (request.startDate() != null) contract.setStartDate(request.startDate());
        if (request.endDate() != null) contract.setEndDate(request.endDate());
        if (request.amount() != null) contract.setAmount(request.amount());
        if (request.status() != null) contract.setStatus(request.status());
        validateDates(contract.getStartDate(), contract.getEndDate());
        contract.setUpdatedAt(Instant.now());
        ProviderContract saved = contractRepository.saveAndFlush(contract);
        ProviderContractResponse response = ProviderContractResponse.from(saved);
        auditService.record(communityId, userId, "USER", "PROVIDER_CONTRACT_UPDATED", "PROVIDER",
                providerId, before, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ProviderBudgetResponse> budgets(UUID communityId, UUID userId, UUID providerId) {
        requireView(communityId, userId, providerId);
        return budgetRepository.findByCommunityIdAndProviderIdOrderByCreatedAtDesc(communityId, providerId)
                .stream().map(ProviderBudgetResponse::from).toList();
    }

    @Transactional
    public ProviderBudgetResponse createBudget(UUID communityId, UUID userId, UUID providerId,
                                              CreateProviderBudgetRequest request,
                                              HttpServletRequest httpRequest) {
        requireManage(communityId, userId, providerId);
        Instant now = Instant.now();
        ProviderBudgetStatus status = request.status() == null
                ? ProviderBudgetStatus.RECEIVED : request.status();
        ProviderBudget budget = budgetRepository.saveAndFlush(new ProviderBudget(
                UUID.randomUUID(), communityId, providerId, request.title().trim(), clean(request.description()),
                request.amount(), request.validUntil(), status, userId, now, now));
        ProviderBudgetResponse response = ProviderBudgetResponse.from(budget);
        auditService.record(communityId, userId, "USER", "PROVIDER_BUDGET_CREATED", "PROVIDER",
                providerId, null, response, httpRequest);
        return response;
    }

    @Transactional
    public ProviderBudgetResponse updateBudget(UUID communityId, UUID userId, UUID providerId,
                                              UUID budgetId, UpdateProviderBudgetRequest request,
                                              HttpServletRequest httpRequest) {
        requireManage(communityId, userId, providerId);
        ProviderBudget budget = budgetRepository
                .findByIdAndCommunityIdAndProviderId(budgetId, communityId, providerId)
                .orElseThrow(() -> BusinessException.notFound("PROVIDER_BUDGET_NOT_FOUND",
                        "errors.resource.notFound"));
        if (request.version() != null && request.version() != budget.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        ProviderBudgetResponse before = ProviderBudgetResponse.from(budget);
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            budget.setTitle(request.title().trim());
        }
        if (request.description() != null) budget.setDescription(clean(request.description()));
        if (request.amount() != null) budget.setAmount(request.amount());
        if (request.validUntil() != null) budget.setValidUntil(request.validUntil());
        if (request.status() != null) budget.setStatus(request.status());
        budget.setUpdatedAt(Instant.now());
        ProviderBudget saved = budgetRepository.saveAndFlush(budget);
        ProviderBudgetResponse response = ProviderBudgetResponse.from(saved);
        auditService.record(communityId, userId, "USER", "PROVIDER_BUDGET_UPDATED", "PROVIDER",
                providerId, before, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ProviderNoteResponse> notes(UUID communityId, UUID userId, UUID providerId) {
        requireView(communityId, userId, providerId);
        return noteRepository.findByCommunityIdAndProviderIdOrderByCreatedAtDesc(communityId, providerId)
                .stream().map(ProviderNoteResponse::from).toList();
    }

    @Transactional
    public ProviderNoteResponse addNote(UUID communityId, UUID userId, UUID providerId,
                                        CreateProviderNoteRequest request,
                                        HttpServletRequest httpRequest) {
        requireManage(communityId, userId, providerId);
        ProviderNote note = noteRepository.saveAndFlush(new ProviderNote(
                UUID.randomUUID(), communityId, providerId, request.body().trim(), userId, Instant.now()));
        ProviderNoteResponse response = ProviderNoteResponse.from(note);
        auditService.record(communityId, userId, "USER", "PROVIDER_NOTE_CREATED", "PROVIDER",
                providerId, null, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ProviderHistoryEntryResponse> history(UUID communityId, UUID userId, UUID providerId) {
        requireView(communityId, userId, providerId);
        List<ProviderHistoryEntryResponse> entries = new ArrayList<>();
        for (ProviderContract contract : contractRepository
                .findByCommunityIdAndProviderIdOrderByCreatedAtDesc(communityId, providerId)) {
            entries.add(new ProviderHistoryEntryResponse("CONTRACT", contract.getId(), contract.getTitle(),
                    contract.getDescription(), contract.getStatus().name(), contract.getAmount(),
                    contract.getCreatedAt()));
        }
        for (ProviderBudget budget : budgetRepository
                .findByCommunityIdAndProviderIdOrderByCreatedAtDesc(communityId, providerId)) {
            entries.add(new ProviderHistoryEntryResponse("BUDGET", budget.getId(), budget.getTitle(),
                    budget.getDescription(), budget.getStatus().name(), budget.getAmount(),
                    budget.getCreatedAt()));
        }
        for (ProviderNote note : noteRepository
                .findByCommunityIdAndProviderIdOrderByCreatedAtDesc(communityId, providerId)) {
            entries.add(new ProviderHistoryEntryResponse("NOTE", note.getId(), null, note.getBody(),
                    null, null, note.getCreatedAt()));
        }
        entries.sort(Comparator.comparing(ProviderHistoryEntryResponse::date).reversed());
        return entries;
    }

    private void requireView(UUID communityId, UUID userId, UUID providerId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "PROVIDER_VIEW");
        ensureProvider(communityId, providerId);
    }

    private void requireManage(UUID communityId, UUID userId, UUID providerId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "PROVIDER_MANAGE");
        ensureProvider(communityId, providerId);
    }

    private void ensureProvider(UUID communityId, UUID providerId) {
        providerRepository.findByIdAndCommunityId(providerId, communityId)
                .orElseThrow(() -> BusinessException.notFound("PROVIDER_NOT_FOUND", "errors.resource.notFound"));
    }

    private void validateDates(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw BusinessException.unprocessable("PROVIDER_CONTRACT_DATES_INVALID", "errors.validation");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

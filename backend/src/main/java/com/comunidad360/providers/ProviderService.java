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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProviderService {
    private final ProviderRepository providerRepository;
    private final ProviderContactRepository contactRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public ProviderService(ProviderRepository providerRepository, ProviderContactRepository contactRepository,
                           TenantAccessService tenantAccessService, PermissionService permissionService,
                           AuditService auditService) {
        this.providerRepository = providerRepository;
        this.contactRepository = contactRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> list(UUID communityId, UUID userId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "PROVIDER_VIEW");
        return providerRepository.findByCommunityIdOrderByNameAsc(communityId).stream()
                .map(ProviderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProviderResponse get(UUID communityId, UUID userId, UUID providerId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "PROVIDER_VIEW");
        return ProviderResponse.from(find(communityId, providerId));
    }

    @Transactional
    public ProviderResponse create(UUID communityId, UUID userId, CreateProviderRequest request,
                                   HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "PROVIDER_MANAGE");
        Instant now = Instant.now();
        Provider provider = providerRepository.saveAndFlush(new Provider(
                UUID.randomUUID(), communityId, request.name().trim(), clean(request.taxId()),
                clean(request.category()), email(request.email()), clean(request.phone()),
                clean(request.address()), clean(request.notes()), userId, now, now));
        auditService.record(communityId, userId, "USER", "PROVIDER_CREATED", "PROVIDER",
                provider.getId(), null, ProviderResponse.from(provider), httpRequest);
        return ProviderResponse.from(provider);
    }

    @Transactional
    public ProviderResponse update(UUID communityId, UUID userId, UUID providerId,
                                   UpdateProviderRequest request, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "PROVIDER_MANAGE");
        Provider provider = find(communityId, providerId);
        if (request.version() != null && request.version() != provider.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        ProviderResponse before = ProviderResponse.from(provider);
        if (request.name() != null) {
            if (request.name().isBlank()) throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            provider.setName(request.name().trim());
        }
        if (request.taxId() != null) provider.setTaxId(clean(request.taxId()));
        if (request.category() != null) provider.setCategory(clean(request.category()));
        if (request.email() != null) provider.setEmail(email(request.email()));
        if (request.phone() != null) provider.setPhone(clean(request.phone()));
        if (request.address() != null) provider.setAddress(clean(request.address()));
        if (request.notes() != null) provider.setNotes(clean(request.notes()));
        if (request.status() != null) provider.setStatus(request.status());
        provider.setUpdatedAt(Instant.now());
        Provider saved = providerRepository.saveAndFlush(provider);
        auditService.record(communityId, userId, "USER", "PROVIDER_UPDATED", "PROVIDER",
                providerId, before, ProviderResponse.from(saved), httpRequest);
        return ProviderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderContactResponse> contacts(UUID communityId, UUID userId, UUID providerId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "PROVIDER_VIEW");
        find(communityId, providerId);
        return contactRepository.findByCommunityIdAndProviderIdOrderByPrimaryContactDescNameAsc(
                communityId, providerId).stream().map(ProviderContactResponse::from).toList();
    }

    @Transactional
    public ProviderContactResponse addContact(UUID communityId, UUID userId, UUID providerId,
                                              CreateProviderContactRequest request,
                                              HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "PROVIDER_MANAGE");
        find(communityId, providerId);
        Instant now = Instant.now();
        if (request.primaryContact()) {
            contactRepository.findByCommunityIdAndProviderId(communityId, providerId).forEach(contact -> {
                contact.setPrimaryContact(false);
                contact.setUpdatedAt(now);
                contactRepository.save(contact);
            });
        }
        ProviderContact contact = contactRepository.saveAndFlush(new ProviderContact(
                UUID.randomUUID(), communityId, providerId, request.name().trim(), clean(request.role()),
                email(request.email()), clean(request.phone()), request.primaryContact(), now, now));
        auditService.record(communityId, userId, "USER", "PROVIDER_CONTACT_CREATED", "PROVIDER",
                providerId, null, ProviderContactResponse.from(contact), httpRequest);
        return ProviderContactResponse.from(contact);
    }

    private Provider find(UUID communityId, UUID providerId) {
        return providerRepository.findByIdAndCommunityId(providerId, communityId)
                .orElseThrow(() -> BusinessException.notFound("PROVIDER_NOT_FOUND", "errors.resource.notFound"));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String email(String value) {
        String clean = clean(value);
        return clean == null ? null : clean.toLowerCase(Locale.ROOT);
    }
}

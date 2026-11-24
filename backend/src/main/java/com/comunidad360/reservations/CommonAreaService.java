package com.comunidad360.reservations;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.reservations.dto.CommonAreaRequest;
import com.comunidad360.reservations.dto.CommonAreaResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CommonAreaService {

    private final CommonAreaRepository repository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public CommonAreaService(CommonAreaRepository repository, TenantAccessService tenantAccessService,
                             PermissionService permissionService, AuditService auditService) {
        this.repository = repository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CommonAreaResponse> list(UUID communityId, UUID userId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "RESERVATION_VIEW");
        boolean manage = permissionService.hasPermission(membership, "AREA_MANAGE");
        List<CommonArea> areas = manage
                ? repository.findByCommunityIdOrderByNameAsc(communityId)
                : repository.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId);
        return areas.stream().map(CommonAreaResponse::from).toList();
    }

    @Transactional
    public CommonAreaResponse create(UUID communityId, UUID userId, CommonAreaRequest request,
                                     HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "AREA_MANAGE");
        Instant now = Instant.now();
        CommonArea area = repository.saveAndFlush(new CommonArea(UUID.randomUUID(), communityId,
                request.name().trim(), clean(request.description()), clean(request.location()),
                request.capacity(), request.requiresApproval() == null || request.requiresApproval(),
                now, now));
        CommonAreaResponse response = CommonAreaResponse.from(area);
        auditService.record(communityId, userId, "USER", "COMMON_AREA_CREATED", "COMMON_AREA",
                area.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public CommonAreaResponse update(UUID communityId, UUID userId, UUID areaId, CommonAreaRequest request,
                                     HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "AREA_MANAGE");
        CommonArea area = repository.findByIdAndCommunityId(areaId, communityId)
                .orElseThrow(() -> BusinessException.notFound("COMMON_AREA_NOT_FOUND", "errors.resource.notFound"));
        CommonAreaResponse before = CommonAreaResponse.from(area);
        area.setName(request.name().trim());
        area.setDescription(clean(request.description()));
        area.setLocation(clean(request.location()));
        area.setCapacity(request.capacity());
        if (request.requiresApproval() != null) area.setRequiresApproval(request.requiresApproval());
        if (request.active() != null) area.setActive(request.active());
        area.setUpdatedAt(Instant.now());
        CommonArea saved = repository.saveAndFlush(area);
        CommonAreaResponse response = CommonAreaResponse.from(saved);
        auditService.record(communityId, userId, "USER", "COMMON_AREA_UPDATED", "COMMON_AREA",
                areaId, before, response, httpRequest);
        return response;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

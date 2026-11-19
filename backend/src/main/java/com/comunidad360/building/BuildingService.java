package com.comunidad360.building;

import com.comunidad360.audit.AuditService;
import com.comunidad360.building.dto.BuildingResponse;
import com.comunidad360.building.dto.CreateBuildingRequest;
import com.comunidad360.building.dto.UpdateBuildingRequest;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.UnitRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public BuildingService(BuildingRepository buildingRepository,
                           UnitRepository unitRepository,
                           TenantAccessService tenantAccessService,
                           AuditService auditService) {
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BuildingResponse> list(UUID communityId, UUID userId) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_VIEW");
        return buildingRepository.findByCommunityIdAndArchivedAtIsNullOrderByNameAsc(communityId).stream()
                .map(BuildingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BuildingResponse get(UUID communityId, UUID userId, UUID buildingId) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_VIEW");
        return BuildingResponse.from(find(communityId, buildingId));
    }

    @Transactional
    public BuildingResponse create(UUID communityId, UUID userId, CreateBuildingRequest request,
                                   HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_MANAGE");
        Instant now = Instant.now();
        Building building = new Building(
                UUID.randomUUID(),
                communityId,
                request.name().trim(),
                now,
                now
        );
        building.setAddress(request.address());
        building.setCity(request.city());
        building.setPostalCode(request.postalCode());
        building.setFloors(request.floors());
        building.setNotes(request.notes());
        Building saved = buildingRepository.saveAndFlush(building);
        BuildingResponse response = BuildingResponse.from(saved);
        auditService.record(communityId, userId, "USER", "BUILDING_CREATED", "BUILDING",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public BuildingResponse update(UUID communityId, UUID userId, UUID buildingId,
                                   UpdateBuildingRequest request, HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_MANAGE");
        Building building = find(communityId, buildingId);
        if (request.version() != null && request.version() != building.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        BuildingResponse before = BuildingResponse.from(building);
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            building.setName(request.name().trim());
        }
        if (request.address() != null) {
            building.setAddress(request.address());
        }
        if (request.city() != null) {
            building.setCity(request.city());
        }
        if (request.postalCode() != null) {
            building.setPostalCode(request.postalCode());
        }
        if (request.floors() != null) {
            building.setFloors(request.floors());
        }
        if (request.notes() != null) {
            building.setNotes(request.notes());
        }
        building.setUpdatedAt(Instant.now());
        Building saved = buildingRepository.saveAndFlush(building);
        BuildingResponse after = BuildingResponse.from(saved);
        auditService.record(communityId, userId, "USER", "BUILDING_UPDATED", "BUILDING",
                buildingId, before, after, httpRequest);
        return after;
    }

    @Transactional
    public void archive(UUID communityId, UUID userId, UUID buildingId, HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_MANAGE");
        Building building = find(communityId, buildingId);
        if (unitRepository.existsByCommunityIdAndBuildingIdAndArchivedAtIsNull(communityId, buildingId)) {
            throw BusinessException.conflict("BUILDING_HAS_UNITS", "errors.building.hasUnits");
        }
        BuildingResponse before = BuildingResponse.from(building);
        building.setArchivedAt(Instant.now());
        building.setUpdatedAt(Instant.now());
        Building saved = buildingRepository.saveAndFlush(building);
        auditService.record(communityId, userId, "USER", "BUILDING_ARCHIVED", "BUILDING",
                buildingId, before, BuildingResponse.from(saved), httpRequest);
    }

    private Building find(UUID communityId, UUID buildingId) {
        return buildingRepository.findByIdAndCommunityId(buildingId, communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "BUILDING_NOT_FOUND", "errors.building.notFound"));
    }
}

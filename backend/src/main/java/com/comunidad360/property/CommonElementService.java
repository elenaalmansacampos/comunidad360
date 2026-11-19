package com.comunidad360.property;

import com.comunidad360.audit.AuditService;
import com.comunidad360.building.Building;
import com.comunidad360.building.BuildingRepository;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.dto.CommonElementResponse;
import com.comunidad360.property.dto.CreateCommonElementRequest;
import com.comunidad360.property.dto.UpdateCommonElementRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CommonElementService {

    private final CommonElementRepository commonElementRepository;
    private final BuildingRepository buildingRepository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public CommonElementService(CommonElementRepository commonElementRepository,
                                BuildingRepository buildingRepository,
                                TenantAccessService tenantAccessService,
                                AuditService auditService) {
        this.commonElementRepository = commonElementRepository;
        this.buildingRepository = buildingRepository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CommonElementResponse> list(UUID communityId, UUID userId) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_VIEW");
        return commonElementRepository.findByCommunityIdAndArchivedAtIsNullOrderByNameAsc(communityId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommonElementResponse get(UUID communityId, UUID userId, UUID elementId) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_VIEW");
        return toResponse(find(communityId, elementId));
    }

    @Transactional
    public CommonElementResponse create(UUID communityId, UUID userId, CreateCommonElementRequest request,
                                        HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_MANAGE");
        validateBuilding(communityId, request.buildingId());
        Instant now = Instant.now();
        CommonElement element = new CommonElement(
                UUID.randomUUID(),
                communityId,
                request.name().trim(),
                now,
                now
        );
        element.setBuildingId(request.buildingId());
        element.setType(request.type());
        element.setDescription(request.description());
        CommonElement saved = commonElementRepository.saveAndFlush(element);
        CommonElementResponse response = toResponse(saved);
        auditService.record(communityId, userId, "USER", "COMMON_ELEMENT_CREATED", "COMMON_ELEMENT",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public CommonElementResponse update(UUID communityId, UUID userId, UUID elementId,
                                        UpdateCommonElementRequest request, HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_MANAGE");
        CommonElement element = find(communityId, elementId);
        CommonElementResponse before = toResponse(element);
        if (request.buildingId() != null) {
            validateBuilding(communityId, request.buildingId());
            element.setBuildingId(request.buildingId());
        }
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            element.setName(request.name().trim());
        }
        if (request.type() != null) {
            element.setType(request.type());
        }
        if (request.description() != null) {
            element.setDescription(request.description());
        }
        element.setUpdatedAt(Instant.now());
        CommonElement saved = commonElementRepository.saveAndFlush(element);
        CommonElementResponse after = toResponse(saved);
        auditService.record(communityId, userId, "USER", "COMMON_ELEMENT_UPDATED", "COMMON_ELEMENT",
                elementId, before, after, httpRequest);
        return after;
    }

    @Transactional
    public void archive(UUID communityId, UUID userId, UUID elementId, HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_MANAGE");
        CommonElement element = find(communityId, elementId);
        CommonElementResponse before = toResponse(element);
        element.setArchivedAt(Instant.now());
        element.setUpdatedAt(Instant.now());
        CommonElement saved = commonElementRepository.saveAndFlush(element);
        auditService.record(communityId, userId, "USER", "COMMON_ELEMENT_ARCHIVED", "COMMON_ELEMENT",
                elementId, before, toResponse(saved), httpRequest);
    }

    private CommonElement find(UUID communityId, UUID elementId) {
        return commonElementRepository.findByIdAndCommunityId(elementId, communityId)
                .filter(element -> element.getArchivedAt() == null)
                .orElseThrow(() -> BusinessException.notFound(
                        "COMMON_ELEMENT_NOT_FOUND", "errors.commonElement.notFound"));
    }

    private CommonElementResponse toResponse(CommonElement element) {
        String buildingName = null;
        if (element.getBuildingId() != null) {
            buildingName = buildingRepository.findById(element.getBuildingId())
                    .map(Building::getName)
                    .orElse(null);
        }
        return CommonElementResponse.from(element, buildingName);
    }

    private void validateBuilding(UUID communityId, UUID buildingId) {
        if (buildingId == null) {
            return;
        }
        Building building = buildingRepository.findByIdAndCommunityId(buildingId, communityId)
                .orElseThrow(() -> BusinessException.unprocessable(
                        "INVALID_BUILDING", "errors.unit.buildingInvalid"));
        if (building.getArchivedAt() != null) {
            throw BusinessException.unprocessable(
                    "INVALID_BUILDING", "errors.unit.buildingInvalid");
        }
    }
}

package com.comunidad360.property;

import com.comunidad360.audit.AuditService;
import com.comunidad360.building.Building;
import com.comunidad360.building.BuildingRepository;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.dto.CreateUnitRequest;
import com.comunidad360.property.dto.UnitResponse;
import com.comunidad360.property.dto.UpdateUnitRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UnitService {

    private final UnitRepository unitRepository;
    private final BuildingRepository buildingRepository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public UnitService(UnitRepository unitRepository,
                       BuildingRepository buildingRepository,
                       TenantAccessService tenantAccessService,
                       AuditService auditService) {
        this.unitRepository = unitRepository;
        this.buildingRepository = buildingRepository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> list(UUID communityId, UUID userId) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_VIEW");
        List<Unit> units = unitRepository.findByCommunityIdAndArchivedAtIsNullOrderByCodeAsc(communityId);
        Map<UUID, String> buildingNames = buildingNames(units);
        return units.stream()
                .map(unit -> UnitResponse.from(unit, buildingName(buildingNames, unit.getBuildingId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UnitResponse get(UUID communityId, UUID userId, UUID unitId) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_VIEW");
        return toResponse(find(communityId, unitId));
    }

    @Transactional
    public UnitResponse create(UUID communityId, UUID userId, CreateUnitRequest request,
                               HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_MANAGE");
        String code = normalizeCode(request.code());
        if (unitRepository.existsByCommunityIdAndCode(communityId, code)) {
            throw BusinessException.conflict("UNIT_CODE_EXISTS", "errors.unit.codeExists");
        }
        validateCoefficient(request.coefficient());
        validateBuilding(communityId, request.buildingId());
        Instant now = Instant.now();
        Unit unit = new Unit(
                UUID.randomUUID(),
                communityId,
                code,
                request.type(),
                request.coefficient(),
                now,
                now
        );
        unit.setBuildingId(request.buildingId());
        unit.setFloor(request.floor());
        unit.setDoor(request.door());
        unit.setBuiltArea(request.builtArea());
        Unit saved = unitRepository.saveAndFlush(unit);
        UnitResponse response = toResponse(saved);
        auditService.record(communityId, userId, "USER", "UNIT_CREATED", "UNIT",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public UnitResponse update(UUID communityId, UUID userId, UUID unitId, UpdateUnitRequest request,
                               HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_MANAGE");
        Unit unit = find(communityId, unitId);
        if (request.version() != null && request.version() != unit.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        UnitResponse before = toResponse(unit);
        if (request.code() != null) {
            String code = normalizeCode(request.code());
            if (unitRepository.existsByCommunityIdAndCodeAndIdNot(communityId, code, unitId)) {
                throw BusinessException.conflict("UNIT_CODE_EXISTS", "errors.unit.codeExists");
            }
            unit.setCode(code);
        }
        if (request.type() != null) {
            unit.setType(request.type());
        }
        if (request.buildingId() != null) {
            validateBuilding(communityId, request.buildingId());
            unit.setBuildingId(request.buildingId());
        }
        if (request.floor() != null) {
            unit.setFloor(request.floor());
        }
        if (request.door() != null) {
            unit.setDoor(request.door());
        }
        if (request.builtArea() != null) {
            unit.setBuiltArea(request.builtArea());
        }
        if (request.coefficient() != null) {
            validateCoefficient(request.coefficient());
            unit.setCoefficient(request.coefficient());
        }
        unit.setUpdatedAt(Instant.now());
        Unit saved = unitRepository.saveAndFlush(unit);
        UnitResponse after = toResponse(saved);
        auditService.record(communityId, userId, "USER", "UNIT_UPDATED", "UNIT",
                unitId, before, after, httpRequest);
        return after;
    }

    @Transactional
    public void archive(UUID communityId, UUID userId, UUID unitId, HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_MANAGE");
        Unit unit = find(communityId, unitId);
        UnitResponse before = toResponse(unit);
        unit.setArchivedAt(Instant.now());
        unit.setStatus(UnitStatus.INACTIVE);
        unit.setUpdatedAt(Instant.now());
        Unit saved = unitRepository.saveAndFlush(unit);
        auditService.record(communityId, userId, "USER", "UNIT_ARCHIVED", "UNIT",
                unitId, before, toResponse(saved), httpRequest);
    }

    private Unit find(UUID communityId, UUID unitId) {
        return unitRepository.findByIdAndCommunityId(unitId, communityId)
                .filter(unit -> unit.getArchivedAt() == null)
                .orElseThrow(() -> BusinessException.notFound(
                        "UNIT_NOT_FOUND", "errors.unit.notFound"));
    }

    private UnitResponse toResponse(Unit unit) {
        String buildingName = null;
        if (unit.getBuildingId() != null) {
            buildingName = buildingRepository.findById(unit.getBuildingId())
                    .map(Building::getName)
                    .orElse(null);
        }
        return UnitResponse.from(unit, buildingName);
    }

    private Map<UUID, String> buildingNames(List<Unit> units) {
        List<UUID> ids = units.stream()
                .map(Unit::getBuildingId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return buildingRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Building::getId, Building::getName, (left, right) -> left));
    }

    private String buildingName(Map<UUID, String> names, UUID buildingId) {
        return buildingId == null ? null : names.get(buildingId);
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
        }
        return code.trim();
    }

    private void validateCoefficient(BigDecimal coefficient) {
        if (coefficient == null
                || coefficient.compareTo(BigDecimal.ZERO) < 0
                || coefficient.compareTo(new BigDecimal("100")) > 0) {
            throw BusinessException.unprocessable(
                    "INVALID_COEFFICIENT", "errors.structure.invalidCoefficient");
        }
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

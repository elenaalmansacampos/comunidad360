package com.comunidad360.property;

import com.comunidad360.building.BuildingRepository;
import com.comunidad360.config.AppProperties;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.dto.StructureSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class StructureSummaryService {

    private final UnitRepository unitRepository;
    private final BuildingRepository buildingRepository;
    private final CommonElementRepository commonElementRepository;
    private final TenantAccessService tenantAccessService;
    private final AppProperties properties;

    public StructureSummaryService(UnitRepository unitRepository,
                                   BuildingRepository buildingRepository,
                                   CommonElementRepository commonElementRepository,
                                   TenantAccessService tenantAccessService,
                                   AppProperties properties) {
        this.unitRepository = unitRepository;
        this.buildingRepository = buildingRepository;
        this.commonElementRepository = commonElementRepository;
        this.tenantAccessService = tenantAccessService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public StructureSummaryResponse summary(UUID communityId, UUID userId) {
        tenantAccessService.requirePermission(communityId, userId, "UNIT_VIEW");
        List<Unit> units = unitRepository.findByCommunityIdAndArchivedAtIsNullOrderByCodeAsc(communityId);
        BigDecimal sum = units.stream()
                .filter(unit -> unit.getStatus() == UnitStatus.ACTIVE)
                .map(Unit::getCoefficient)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tolerance = properties.getStructure().getCoefficientTolerance();
        boolean valid = sum.subtract(new BigDecimal("100")).abs().compareTo(tolerance) <= 0;
        long activeUnits = units.stream().filter(unit -> unit.getStatus() == UnitStatus.ACTIVE).count();
        return new StructureSummaryResponse(
                sum,
                tolerance,
                valid,
                units.size(),
                activeUnits,
                buildingRepository.countByCommunityIdAndArchivedAtIsNull(communityId),
                commonElementRepository.countByCommunityIdAndArchivedAtIsNull(communityId)
        );
    }
}

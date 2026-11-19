package com.comunidad360.building;

import com.comunidad360.building.dto.BuildingResponse;
import com.comunidad360.building.dto.CreateBuildingRequest;
import com.comunidad360.building.dto.UpdateBuildingRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping
    public List<BuildingResponse> list(@PathVariable UUID communityId,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return buildingService.list(communityId, user.id());
    }

    @GetMapping("/{buildingId}")
    public BuildingResponse get(@PathVariable UUID communityId,
                                @PathVariable UUID buildingId,
                                @AuthenticationPrincipal AuthenticatedUser user) {
        return buildingService.get(communityId, user.id(), buildingId);
    }

    @PostMapping
    public ResponseEntity<BuildingResponse> create(@PathVariable UUID communityId,
                                                   @AuthenticationPrincipal AuthenticatedUser user,
                                                   @Valid @RequestBody CreateBuildingRequest request,
                                                   HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildingService.create(communityId, user.id(), request, httpRequest));
    }

    @PatchMapping("/{buildingId}")
    public BuildingResponse update(@PathVariable UUID communityId,
                                   @PathVariable UUID buildingId,
                                   @AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody UpdateBuildingRequest request,
                                   HttpServletRequest httpRequest) {
        return buildingService.update(communityId, user.id(), buildingId, request, httpRequest);
    }

    @DeleteMapping("/{buildingId}")
    public ResponseEntity<Void> archive(@PathVariable UUID communityId,
                                        @PathVariable UUID buildingId,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        HttpServletRequest httpRequest) {
        buildingService.archive(communityId, user.id(), buildingId, httpRequest);
        return ResponseEntity.noContent().build();
    }
}

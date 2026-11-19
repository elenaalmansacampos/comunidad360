package com.comunidad360.property;

import com.comunidad360.property.dto.CreateUnitRequest;
import com.comunidad360.property.dto.UnitResponse;
import com.comunidad360.property.dto.UpdateUnitRequest;
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
@RequestMapping("/api/v1/communities/{communityId}/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    public List<UnitResponse> list(@PathVariable UUID communityId,
                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return unitService.list(communityId, user.id());
    }

    @GetMapping("/{unitId}")
    public UnitResponse get(@PathVariable UUID communityId,
                            @PathVariable UUID unitId,
                            @AuthenticationPrincipal AuthenticatedUser user) {
        return unitService.get(communityId, user.id(), unitId);
    }

    @PostMapping
    public ResponseEntity<UnitResponse> create(@PathVariable UUID communityId,
                                               @AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CreateUnitRequest request,
                                               HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(unitService.create(communityId, user.id(), request, httpRequest));
    }

    @PatchMapping("/{unitId}")
    public UnitResponse update(@PathVariable UUID communityId,
                               @PathVariable UUID unitId,
                               @AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody UpdateUnitRequest request,
                               HttpServletRequest httpRequest) {
        return unitService.update(communityId, user.id(), unitId, request, httpRequest);
    }

    @DeleteMapping("/{unitId}")
    public ResponseEntity<Void> archive(@PathVariable UUID communityId,
                                        @PathVariable UUID unitId,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        HttpServletRequest httpRequest) {
        unitService.archive(communityId, user.id(), unitId, httpRequest);
        return ResponseEntity.noContent().build();
    }
}

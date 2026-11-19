package com.comunidad360.property;

import com.comunidad360.property.dto.CreateOwnershipRequest;
import com.comunidad360.property.dto.OwnershipResponse;
import com.comunidad360.property.dto.UpdateOwnershipRequest;
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
@RequestMapping("/api/v1/communities/{communityId}/units/{unitId}/ownerships")
public class OwnershipController {

    private final OwnershipService ownershipService;

    public OwnershipController(OwnershipService ownershipService) {
        this.ownershipService = ownershipService;
    }

    @GetMapping
    public List<OwnershipResponse> list(@PathVariable UUID communityId,
                                        @PathVariable UUID unitId,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return ownershipService.list(communityId, user.id(), unitId);
    }

    @PostMapping
    public ResponseEntity<OwnershipResponse> create(@PathVariable UUID communityId,
                                                    @PathVariable UUID unitId,
                                                    @AuthenticationPrincipal AuthenticatedUser user,
                                                    @Valid @RequestBody CreateOwnershipRequest request,
                                                    HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ownershipService.create(communityId, user.id(), unitId, request, httpRequest));
    }

    @PatchMapping("/{ownershipId}")
    public OwnershipResponse update(@PathVariable UUID communityId,
                                    @PathVariable UUID unitId,
                                    @PathVariable UUID ownershipId,
                                    @AuthenticationPrincipal AuthenticatedUser user,
                                    @Valid @RequestBody UpdateOwnershipRequest request,
                                    HttpServletRequest httpRequest) {
        return ownershipService.update(communityId, user.id(), unitId, ownershipId, request, httpRequest);
    }

    @DeleteMapping("/{ownershipId}")
    public ResponseEntity<Void> end(@PathVariable UUID communityId,
                                    @PathVariable UUID unitId,
                                    @PathVariable UUID ownershipId,
                                    @AuthenticationPrincipal AuthenticatedUser user,
                                    HttpServletRequest httpRequest) {
        ownershipService.end(communityId, user.id(), unitId, ownershipId, httpRequest);
        return ResponseEntity.noContent().build();
    }
}

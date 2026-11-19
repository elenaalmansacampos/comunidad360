package com.comunidad360.property;

import com.comunidad360.property.dto.CreateTenancyRequest;
import com.comunidad360.property.dto.TenancyResponse;
import com.comunidad360.property.dto.UpdateTenancyRequest;
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
@RequestMapping("/api/v1/communities/{communityId}/units/{unitId}/tenancies")
public class TenancyController {

    private final TenancyService tenancyService;

    public TenancyController(TenancyService tenancyService) {
        this.tenancyService = tenancyService;
    }

    @GetMapping
    public List<TenancyResponse> list(@PathVariable UUID communityId,
                                      @PathVariable UUID unitId,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return tenancyService.list(communityId, user.id(), unitId);
    }

    @PostMapping
    public ResponseEntity<TenancyResponse> create(@PathVariable UUID communityId,
                                                  @PathVariable UUID unitId,
                                                  @AuthenticationPrincipal AuthenticatedUser user,
                                                  @Valid @RequestBody CreateTenancyRequest request,
                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenancyService.create(communityId, user.id(), unitId, request, httpRequest));
    }

    @PatchMapping("/{tenancyId}")
    public TenancyResponse update(@PathVariable UUID communityId,
                                  @PathVariable UUID unitId,
                                  @PathVariable UUID tenancyId,
                                  @AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody UpdateTenancyRequest request,
                                  HttpServletRequest httpRequest) {
        return tenancyService.update(communityId, user.id(), unitId, tenancyId, request, httpRequest);
    }

    @DeleteMapping("/{tenancyId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID communityId,
                                       @PathVariable UUID unitId,
                                       @PathVariable UUID tenancyId,
                                       @AuthenticationPrincipal AuthenticatedUser user,
                                       HttpServletRequest httpRequest) {
        tenancyService.revoke(communityId, user.id(), unitId, tenancyId, httpRequest);
        return ResponseEntity.noContent().build();
    }
}

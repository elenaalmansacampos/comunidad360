package com.comunidad360.providers;

import com.comunidad360.providers.dto.*;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/providers")
public class ProviderController {
    private final ProviderService providerService;
    public ProviderController(ProviderService providerService) { this.providerService = providerService; }

    @GetMapping
    public List<ProviderResponse> list(@PathVariable UUID communityId,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.list(communityId, user.id());
    }

    @PostMapping
    public ResponseEntity<ProviderResponse> create(@PathVariable UUID communityId,
                                                   @AuthenticationPrincipal AuthenticatedUser user,
                                                   @Valid @RequestBody CreateProviderRequest request,
                                                   HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.create(communityId, user.id(), request, httpRequest));
    }

    @GetMapping("/{providerId}")
    public ProviderResponse get(@PathVariable UUID communityId, @PathVariable UUID providerId,
                                @AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.get(communityId, user.id(), providerId);
    }

    @PatchMapping("/{providerId}")
    public ProviderResponse update(@PathVariable UUID communityId, @PathVariable UUID providerId,
                                   @AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody UpdateProviderRequest request,
                                   HttpServletRequest httpRequest) {
        return providerService.update(communityId, user.id(), providerId, request, httpRequest);
    }

    @GetMapping("/{providerId}/contacts")
    public List<ProviderContactResponse> contacts(@PathVariable UUID communityId,
                                                  @PathVariable UUID providerId,
                                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.contacts(communityId, user.id(), providerId);
    }

    @PostMapping("/{providerId}/contacts")
    public ResponseEntity<ProviderContactResponse> addContact(
            @PathVariable UUID communityId, @PathVariable UUID providerId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateProviderContactRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.addContact(communityId, user.id(), providerId, request, httpRequest));
    }
}

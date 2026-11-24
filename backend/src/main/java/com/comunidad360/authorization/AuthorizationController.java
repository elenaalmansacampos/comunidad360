package com.comunidad360.authorization;

import com.comunidad360.authorization.dto.AuthorizationResponse;
import com.comunidad360.authorization.dto.CreateAuthorizationRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/authorizations")
public class AuthorizationController {

    private final AuthorizationService service;

    public AuthorizationController(AuthorizationService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuthorizationResponse> list(@PathVariable UUID communityId,
                                            @AuthenticationPrincipal AuthenticatedUser user,
                                            @RequestParam(defaultValue = "ALL") String direction,
                                            @RequestParam(defaultValue = "false") boolean all) {
        return service.list(communityId, user.id(), direction, all);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorizationResponse create(@PathVariable UUID communityId,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody CreateAuthorizationRequest request,
                                        HttpServletRequest httpRequest) {
        return service.create(communityId, user.id(), request, httpRequest);
    }

    @DeleteMapping("/{authorizationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID communityId, @PathVariable UUID authorizationId,
                       @AuthenticationPrincipal AuthenticatedUser user,
                       HttpServletRequest httpRequest) {
        service.revoke(communityId, user.id(), authorizationId, httpRequest);
    }
}

package com.comunidad360.reservations;

import com.comunidad360.reservations.dto.CommonAreaRequest;
import com.comunidad360.reservations.dto.CommonAreaResponse;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/common-areas")
public class CommonAreaController {

    private final CommonAreaService service;

    public CommonAreaController(CommonAreaService service) {
        this.service = service;
    }

    @GetMapping
    public List<CommonAreaResponse> list(@PathVariable UUID communityId,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(communityId, user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonAreaResponse create(@PathVariable UUID communityId,
                                     @AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody CommonAreaRequest request,
                                     HttpServletRequest httpRequest) {
        return service.create(communityId, user.id(), request, httpRequest);
    }

    @PatchMapping("/{areaId}")
    public CommonAreaResponse update(@PathVariable UUID communityId, @PathVariable UUID areaId,
                                     @AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody CommonAreaRequest request,
                                     HttpServletRequest httpRequest) {
        return service.update(communityId, user.id(), areaId, request, httpRequest);
    }
}

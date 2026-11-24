package com.comunidad360.lostfound;

import com.comunidad360.lostfound.dto.CreateLostItemRequest;
import com.comunidad360.lostfound.dto.LostItemResponse;
import com.comunidad360.lostfound.dto.UpdateLostItemRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/lost-items")
public class LostItemController {

    private final LostItemService service;

    public LostItemController(LostItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<LostItemResponse> list(@PathVariable UUID communityId,
                                       @RequestParam(required = false) LostItemType type,
                                       @RequestParam(required = false) LostItemStatus status,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(communityId, user.id(), type, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LostItemResponse create(@PathVariable UUID communityId,
                                   @AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody CreateLostItemRequest request,
                                   HttpServletRequest httpRequest) {
        return service.create(communityId, user.id(), request, httpRequest);
    }

    @PatchMapping("/{itemId}")
    public LostItemResponse update(@PathVariable UUID communityId, @PathVariable UUID itemId,
                                   @AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody UpdateLostItemRequest request,
                                   HttpServletRequest httpRequest) {
        return service.update(communityId, user.id(), itemId, request, httpRequest);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID communityId, @PathVariable UUID itemId,
                       @AuthenticationPrincipal AuthenticatedUser user,
                       HttpServletRequest httpRequest) {
        service.delete(communityId, user.id(), itemId, httpRequest);
    }
}

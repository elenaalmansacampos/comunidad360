package com.comunidad360.directory;

import com.comunidad360.directory.dto.DirectoryEntryResponse;
import com.comunidad360.directory.dto.UpdateDirectoryProfileRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/directory")
public class DirectoryController {

    private final DirectoryService service;

    public DirectoryController(DirectoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<DirectoryEntryResponse> list(@PathVariable UUID communityId,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(communityId, user.id());
    }

    @PutMapping("/me")
    public DirectoryEntryResponse updateMe(@PathVariable UUID communityId,
                                           @AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody UpdateDirectoryProfileRequest request,
                                           HttpServletRequest httpRequest) {
        return service.updateMe(communityId, user.id(), request, httpRequest);
    }
}

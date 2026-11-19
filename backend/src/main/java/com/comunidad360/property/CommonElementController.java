package com.comunidad360.property;

import com.comunidad360.property.dto.CommonElementResponse;
import com.comunidad360.property.dto.CreateCommonElementRequest;
import com.comunidad360.property.dto.UpdateCommonElementRequest;
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
@RequestMapping("/api/v1/communities/{communityId}/common-elements")
public class CommonElementController {

    private final CommonElementService commonElementService;

    public CommonElementController(CommonElementService commonElementService) {
        this.commonElementService = commonElementService;
    }

    @GetMapping
    public List<CommonElementResponse> list(@PathVariable UUID communityId,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        return commonElementService.list(communityId, user.id());
    }

    @GetMapping("/{elementId}")
    public CommonElementResponse get(@PathVariable UUID communityId,
                                     @PathVariable UUID elementId,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return commonElementService.get(communityId, user.id(), elementId);
    }

    @PostMapping
    public ResponseEntity<CommonElementResponse> create(@PathVariable UUID communityId,
                                                        @AuthenticationPrincipal AuthenticatedUser user,
                                                        @Valid @RequestBody CreateCommonElementRequest request,
                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commonElementService.create(communityId, user.id(), request, httpRequest));
    }

    @PatchMapping("/{elementId}")
    public CommonElementResponse update(@PathVariable UUID communityId,
                                        @PathVariable UUID elementId,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody UpdateCommonElementRequest request,
                                        HttpServletRequest httpRequest) {
        return commonElementService.update(communityId, user.id(), elementId, request, httpRequest);
    }

    @DeleteMapping("/{elementId}")
    public ResponseEntity<Void> archive(@PathVariable UUID communityId,
                                        @PathVariable UUID elementId,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        HttpServletRequest httpRequest) {
        commonElementService.archive(communityId, user.id(), elementId, httpRequest);
        return ResponseEntity.noContent().build();
    }
}

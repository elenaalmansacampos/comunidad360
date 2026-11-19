package com.comunidad360.property;

import com.comunidad360.property.dto.StructureSummaryResponse;
import com.comunidad360.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/structure")
public class StructureController {

    private final StructureSummaryService summaryService;

    public StructureController(StructureSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public StructureSummaryResponse summary(@PathVariable UUID communityId,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        return summaryService.summary(communityId, user.id());
    }
}

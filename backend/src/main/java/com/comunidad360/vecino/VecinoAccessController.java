package com.comunidad360.vecino;

import com.comunidad360.security.AuthenticatedUser;
import com.comunidad360.vecino.dto.VecinoAccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/vecino-access")
public class VecinoAccessController {

    private final VecinoAccessService service;

    public VecinoAccessController(VecinoAccessService service) {
        this.service = service;
    }

    @PostMapping
    public VecinoAccessResponse generate(@PathVariable UUID communityId,
                                         @AuthenticationPrincipal AuthenticatedUser user,
                                         HttpServletRequest request) {
        return service.generate(communityId, user.id(), request);
    }
}

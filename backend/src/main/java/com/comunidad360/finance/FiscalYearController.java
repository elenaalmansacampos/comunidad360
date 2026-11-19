package com.comunidad360.finance;

import com.comunidad360.finance.dto.CreateFiscalYearRequest;
import com.comunidad360.finance.dto.FiscalYearResponse;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/finance/fiscal-years")
public class FiscalYearController {

    private final FiscalYearService fiscalYearService;

    public FiscalYearController(FiscalYearService fiscalYearService) {
        this.fiscalYearService = fiscalYearService;
    }

    @GetMapping
    public List<FiscalYearResponse> list(@PathVariable UUID communityId,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return fiscalYearService.list(communityId, user.id());
    }

    @PostMapping
    public ResponseEntity<FiscalYearResponse> create(@PathVariable UUID communityId,
                                                     @AuthenticationPrincipal AuthenticatedUser user,
                                                     @Valid @RequestBody CreateFiscalYearRequest request,
                                                     HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fiscalYearService.create(communityId, user.id(), request, httpRequest));
    }

    @PostMapping("/{fiscalYearId}/close")
    public FiscalYearResponse close(@PathVariable UUID communityId,
                                    @PathVariable UUID fiscalYearId,
                                    @AuthenticationPrincipal AuthenticatedUser user,
                                    HttpServletRequest httpRequest) {
        return fiscalYearService.close(communityId, user.id(), fiscalYearId, httpRequest);
    }
}

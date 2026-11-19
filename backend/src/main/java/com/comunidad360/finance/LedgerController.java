package com.comunidad360.finance;

import com.comunidad360.finance.dto.CreateLedgerEntryRequest;
import com.comunidad360.finance.dto.LedgerEntryResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/finance/ledger-entries")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    public List<LedgerEntryResponse> list(@PathVariable UUID communityId,
                                          @RequestParam(required = false) UUID fiscalYearId,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        return ledgerService.list(communityId, user.id(), fiscalYearId);
    }

    @PostMapping
    public ResponseEntity<LedgerEntryResponse> create(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateLedgerEntryRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ledgerService.create(communityId, user.id(), request, idempotencyKey, httpRequest));
    }

    @PostMapping("/{entryId}/post")
    public LedgerEntryResponse post(@PathVariable UUID communityId,
                                    @PathVariable UUID entryId,
                                    @AuthenticationPrincipal AuthenticatedUser user,
                                    HttpServletRequest httpRequest) {
        return ledgerService.post(communityId, user.id(), entryId, httpRequest);
    }

    @PostMapping("/{entryId}/reverse")
    public LedgerEntryResponse reverse(@PathVariable UUID communityId,
                                       @PathVariable UUID entryId,
                                       @AuthenticationPrincipal AuthenticatedUser user,
                                       HttpServletRequest httpRequest) {
        return ledgerService.reverse(communityId, user.id(), entryId, httpRequest);
    }
}

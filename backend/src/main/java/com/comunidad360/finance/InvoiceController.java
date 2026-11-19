package com.comunidad360.finance;

import com.comunidad360.finance.dto.CreateInvoiceRequest;
import com.comunidad360.finance.dto.InvoiceResponse;
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
@RequestMapping("/api/v1/communities/{communityId}/finance/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<InvoiceResponse> list(@PathVariable UUID communityId,
                                      @RequestParam(required = false) UUID fiscalYearId,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return invoiceService.list(communityId, user.id(), fiscalYearId);
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateInvoiceRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.create(communityId, user.id(), request, idempotencyKey, httpRequest));
    }
}

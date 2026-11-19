package com.comunidad360.banking;

import com.comunidad360.banking.dto.BankAccountResponse;
import com.comunidad360.banking.dto.BankImportBatchResponse;
import com.comunidad360.banking.dto.BankTransactionResponse;
import com.comunidad360.banking.dto.CreateBankAccountRequest;
import com.comunidad360.banking.dto.ReconcileRequest;
import com.comunidad360.banking.dto.ReconciliationResponse;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/finance/banking")
public class BankingController {

    private final BankService bankService;

    public BankingController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/accounts")
    public List<BankAccountResponse> accounts(@PathVariable UUID communityId,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        return bankService.listAccounts(communityId, user.id());
    }

    @PostMapping("/accounts")
    public ResponseEntity<BankAccountResponse> createAccount(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateBankAccountRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bankService.createAccount(communityId, user.id(), request, httpRequest));
    }

    @GetMapping("/imports")
    public List<BankImportBatchResponse> imports(@PathVariable UUID communityId,
                                                 @AuthenticationPrincipal AuthenticatedUser user) {
        return bankService.listImports(communityId, user.id());
    }

    @PostMapping("/imports")
    public ResponseEntity<BankImportBatchResponse> importCsv(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam UUID accountId,
            @RequestParam String dateColumn,
            @RequestParam String descriptionColumn,
            @RequestParam String amountColumn,
            @RequestParam(required = false) String balanceColumn,
            @RequestParam(required = false) String externalIdColumn,
            @RequestParam(required = false, defaultValue = ",") String separator,
            @RequestParam(required = false) String dateFormat,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bankService.importCsv(
                        communityId,
                        user.id(),
                        accountId,
                        file,
                        dateColumn,
                        descriptionColumn,
                        amountColumn,
                        balanceColumn,
                        externalIdColumn,
                        separator,
                        dateFormat,
                        httpRequest
                ));
    }

    @GetMapping("/transactions")
    public List<BankTransactionResponse> transactions(@PathVariable UUID communityId,
                                                      @RequestParam(required = false) UUID accountId,
                                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return bankService.listTransactions(communityId, user.id(), accountId);
    }

    @PostMapping("/transactions/{transactionId}/reconcile")
    public ReconciliationResponse reconcile(@PathVariable UUID communityId,
                                            @PathVariable UUID transactionId,
                                            @AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody ReconcileRequest request,
                                            HttpServletRequest httpRequest) {
        return bankService.reconcile(communityId, user.id(), transactionId, request, httpRequest);
    }

    @DeleteMapping("/reconciliations/{reconciliationId}")
    public ResponseEntity<Void> unreconcile(@PathVariable UUID communityId,
                                            @PathVariable UUID reconciliationId,
                                            @AuthenticationPrincipal AuthenticatedUser user,
                                            HttpServletRequest httpRequest) {
        bankService.unreconcile(communityId, user.id(), reconciliationId, httpRequest);
        return ResponseEntity.noContent().build();
    }
}

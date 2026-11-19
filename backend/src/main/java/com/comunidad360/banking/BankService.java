package com.comunidad360.banking;

import com.comunidad360.audit.AuditService;
import com.comunidad360.banking.dto.BankAccountResponse;
import com.comunidad360.banking.dto.BankImportBatchResponse;
import com.comunidad360.banking.dto.BankTransactionResponse;
import com.comunidad360.banking.dto.CreateBankAccountRequest;
import com.comunidad360.banking.dto.ReconcileRequest;
import com.comunidad360.banking.dto.ReconciliationResponse;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.config.AppProperties;
import com.comunidad360.finance.FinanceAccessService;
import com.comunidad360.finance.LedgerEntry;
import com.comunidad360.finance.LedgerEntryRepository;
import com.comunidad360.finance.LedgerEntryStatus;
import com.comunidad360.finance.Payment;
import com.comunidad360.finance.PaymentRepository;
import com.comunidad360.finance.PaymentStatus;
import com.comunidad360.security.TokenSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BankService {

    private final BankAccountRepository accountRepository;
    private final BankImportBatchRepository importBatchRepository;
    private final BankTransactionRepository transactionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final PaymentRepository paymentRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final FinanceAccessService financeAccessService;
    private final BankCsvParser csvParser;
    private final AuditService auditService;
    private final AppProperties properties;

    public BankService(BankAccountRepository accountRepository,
                       BankImportBatchRepository importBatchRepository,
                       BankTransactionRepository transactionRepository,
                       ReconciliationRepository reconciliationRepository,
                       PaymentRepository paymentRepository,
                       LedgerEntryRepository ledgerEntryRepository,
                       FinanceAccessService financeAccessService,
                       BankCsvParser csvParser,
                       AuditService auditService,
                       AppProperties properties) {
        this.accountRepository = accountRepository;
        this.importBatchRepository = importBatchRepository;
        this.transactionRepository = transactionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.paymentRepository = paymentRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.financeAccessService = financeAccessService;
        this.csvParser = csvParser;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> listAccounts(UUID communityId, UUID userId) {
        financeAccessService.requireStaff(communityId, userId, "BANK_ACCOUNT_VIEW");
        return accountRepository.findByCommunityIdOrderByNameAsc(communityId).stream()
                .map(BankAccountResponse::from)
                .toList();
    }

    @Transactional
    public BankAccountResponse createAccount(UUID communityId, UUID userId, CreateBankAccountRequest request,
                                             HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "FINANCE_MANAGE");
        String iban = request.iban() == null
                ? ""
                : request.iban().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (iban.length() < 15 || iban.length() > 34) {
            throw BusinessException.unprocessable("INVALID_IBAN", "errors.validation");
        }
        String last4 = iban.substring(iban.length() - 4);
        Instant now = Instant.now();
        BankAccount account = new BankAccount(
                UUID.randomUUID(),
                communityId,
                request.name().trim(),
                request.bankName(),
                last4,
                "**** **** **** " + last4,
                BankAccountStatus.ACTIVE,
                BankAccountSource.MANUAL,
                now,
                now
        );
        BankAccount saved = accountRepository.saveAndFlush(account);
        BankAccountResponse response = BankAccountResponse.from(saved);
        auditService.record(communityId, userId, "USER", "BANK_ACCOUNT_CREATED", "BANK_ACCOUNT",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public List<BankImportBatchResponse> listImports(UUID communityId, UUID userId) {
        financeAccessService.requireStaff(communityId, userId, "BANK_IMPORT");
        return importBatchRepository.findByCommunityIdOrderByImportedAtDesc(communityId).stream()
                .map(BankImportBatchResponse::from)
                .toList();
    }

    @Transactional
    public BankImportBatchResponse importCsv(UUID communityId, UUID userId, UUID accountId,
                                             MultipartFile file, String dateColumn, String descriptionColumn,
                                             String amountColumn, String balanceColumn, String externalIdColumn,
                                             String separator, String dateFormat,
                                             HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "BANK_IMPORT");
        BankAccount account = accountRepository.findByIdAndCommunityId(accountId, communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "BANK_ACCOUNT_NOT_FOUND", "errors.bank.accountNotFound"));
        if (file == null || file.isEmpty()) {
            throw BusinessException.unprocessable("BANK_IMPORT_EMPTY", "errors.bank.importEmpty");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read CSV file", exception);
        }
        BankCsvParser.ParseResult parsed = csvParser.parse(content, new BankCsvParser.Mapping(
                dateColumn,
                descriptionColumn,
                amountColumn,
                balanceColumn,
                externalIdColumn,
                separator,
                dateFormat
        ));
        if (parsed.totalRows() > properties.getBank().getImportMaxRows()) {
            throw BusinessException.unprocessable("BANK_IMPORT_TOO_LARGE", "errors.bank.importTooLarge");
        }

        Instant now = Instant.now();
        String fileName = safeFileName(file.getOriginalFilename());
        BankImportBatch batch = new BankImportBatch(
                UUID.randomUUID(),
                communityId,
                account.getId(),
                fileName,
                TokenSupport.sha256Hex(content),
                userId,
                now,
                BankImportStatus.PROCESSING
        );
        importBatchRepository.save(batch);

        int success = 0;
        int duplicates = 0;
        for (BankCsvParser.ParsedRow row : parsed.rows()) {
            String rawHash = TokenSupport.sha256Hex(
                    account.getId() + "|" + row.valueDate() + "|" + row.description() + "|"
                            + row.amount().toPlainString() + "|" + row.externalId());
            if (transactionRepository.existsByBankAccountIdAndRawHash(account.getId(), rawHash)) {
                duplicates++;
                continue;
            }
            BankTransaction transaction = new BankTransaction(
                    UUID.randomUUID(),
                    communityId,
                    account.getId(),
                    batch.getId(),
                    row.externalId(),
                    row.valueDate(),
                    null,
                    row.description(),
                    row.amount(),
                    row.balanceAfter(),
                    rawHash,
                    now
            );
            transactionRepository.save(transaction);
            success++;
        }
        batch.setRowCount(parsed.totalRows());
        batch.setSuccessCount(success);
        batch.setDuplicateCount(duplicates);
        batch.setErrorCount(parsed.errorCount());
        batch.setStatus(BankImportStatus.COMPLETED);
        BankImportBatch saved = importBatchRepository.saveAndFlush(batch);
        BankImportBatchResponse response = BankImportBatchResponse.from(saved);
        auditService.record(communityId, userId, "USER", "BANK_IMPORT_COMPLETED", "BANK_IMPORT",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public List<BankTransactionResponse> listTransactions(UUID communityId, UUID userId, UUID accountId) {
        financeAccessService.requireStaff(communityId, userId, "BANK_ACCOUNT_VIEW");
        List<BankTransaction> transactions = accountId == null
                ? transactionRepository.findByCommunityIdOrderByValueDateDesc(communityId)
                : transactionRepository.findByCommunityIdAndBankAccountIdOrderByValueDateDesc(
                        communityId, accountId);
        Map<UUID, Reconciliation> reconciliations = reconciliationRepository.findByCommunityId(communityId).stream()
                .collect(Collectors.toMap(
                        Reconciliation::getBankTransactionId,
                        Function.identity(),
                        (left, right) -> left
                ));
        return transactions.stream()
                .map(transaction -> toResponse(transaction, reconciliations.get(transaction.getId())))
                .toList();
    }

    @Transactional
    public ReconciliationResponse reconcile(UUID communityId, UUID userId, UUID transactionId,
                                              ReconcileRequest request, HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "BANK_RECONCILE");
        BankTransaction transaction = transactionRepository.findByIdAndCommunityId(transactionId, communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "BANK_TRANSACTION_NOT_FOUND", "errors.bank.transactionNotFound"));
        if (transaction.getStatus() != BankTransactionStatus.UNRECONCILED) {
            throw BusinessException.conflict("BANK_ALREADY_RECONCILED", "errors.bank.alreadyReconciled");
        }
        boolean hasPayment = request.paymentId() != null;
        boolean hasLedger = request.ledgerEntryId() != null;
        if (hasPayment == hasLedger) {
            throw BusinessException.unprocessable(
                    "BANK_RECONCILIATION_TARGET_REQUIRED", "errors.bank.targetRequired");
        }
        BigDecimal expected = transaction.getAmount().abs();
        BigDecimal matched = request.matchedAmount() == null ? expected : request.matchedAmount();
        if (matched.compareTo(expected) != 0) {
            throw BusinessException.unprocessable(
                    "BANK_RECONCILIATION_AMOUNT_MISMATCH", "errors.bank.amountMismatch");
        }

        if (hasPayment) {
            Payment payment = paymentRepository.findByIdAndCommunityId(request.paymentId(), communityId)
                    .orElseThrow(() -> BusinessException.unprocessable(
                            "BANK_PAYMENT_INVALID", "errors.bank.paymentInvalid"));
            if (payment.getStatus() != PaymentStatus.REGISTERED
                    || reconciliationRepository.existsByCommunityIdAndPaymentId(communityId, payment.getId())) {
                throw BusinessException.unprocessable(
                        "BANK_PAYMENT_INVALID", "errors.bank.paymentInvalid");
            }
            payment.setStatus(PaymentStatus.RECONCILED);
            payment.setUpdatedAt(Instant.now());
            paymentRepository.save(payment);
        } else {
            LedgerEntry entry = ledgerEntryRepository.findByIdAndCommunityId(request.ledgerEntryId(), communityId)
                    .orElseThrow(() -> BusinessException.unprocessable(
                            "BANK_LEDGER_INVALID", "errors.bank.ledgerInvalid"));
            if (entry.getStatus() != LedgerEntryStatus.POSTED
                    || reconciliationRepository.existsByCommunityIdAndLedgerEntryId(communityId, entry.getId())) {
                throw BusinessException.unprocessable(
                        "BANK_LEDGER_INVALID", "errors.bank.ledgerInvalid");
            }
        }

        Instant now = Instant.now();
        Reconciliation reconciliation = new Reconciliation(
                UUID.randomUUID(),
                communityId,
                transactionId,
                request.paymentId(),
                request.ledgerEntryId(),
                matched,
                userId,
                now,
                request.notes()
        );
        Reconciliation saved = reconciliationRepository.saveAndFlush(reconciliation);
        transaction.setStatus(BankTransactionStatus.RECONCILED);
        transaction.setUpdatedAt(now);
        transactionRepository.save(transaction);
        ReconciliationResponse response = ReconciliationResponse.from(saved);
        auditService.record(communityId, userId, "USER", "BANK_TRANSACTION_RECONCILED", "BANK_TRANSACTION",
                transactionId, null, response, httpRequest);
        return response;
    }

    @Transactional
    public void unreconcile(UUID communityId, UUID userId, UUID reconciliationId,
                            HttpServletRequest httpRequest) {
        financeAccessService.requireStaff(communityId, userId, "BANK_RECONCILE");
        Reconciliation reconciliation = reconciliationRepository.findByIdAndCommunityId(
                        reconciliationId, communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "BANK_RECONCILIATION_NOT_FOUND", "errors.bank.reconciliationNotFound"));
        BankTransaction transaction = transactionRepository.findByIdAndCommunityId(
                        reconciliation.getBankTransactionId(), communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "BANK_TRANSACTION_NOT_FOUND", "errors.bank.transactionNotFound"));
        transaction.setStatus(BankTransactionStatus.UNRECONCILED);
        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);
        if (reconciliation.getPaymentId() != null) {
            paymentRepository.findByIdAndCommunityId(reconciliation.getPaymentId(), communityId)
                    .ifPresent(payment -> {
                        payment.setStatus(PaymentStatus.REGISTERED);
                        payment.setUpdatedAt(Instant.now());
                        paymentRepository.save(payment);
                    });
        }
        auditService.record(communityId, userId, "USER", "BANK_TRANSACTION_UNRECONCILED",
                "BANK_TRANSACTION", transaction.getId(), ReconciliationResponse.from(reconciliation),
                null, httpRequest);
        reconciliationRepository.delete(reconciliation);
    }

    private BankTransactionResponse toResponse(BankTransaction transaction, Reconciliation reconciliation) {
        return BankTransactionResponse.from(
                transaction,
                reconciliation == null ? null : reconciliation.getId(),
                reconciliation == null ? null : reconciliation.getPaymentId(),
                reconciliation == null ? null : reconciliation.getLedgerEntryId()
        );
    }

    private String safeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "import.csv";
        }
        String name = original.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

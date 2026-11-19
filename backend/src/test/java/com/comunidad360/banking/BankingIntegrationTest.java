package com.comunidad360.banking;

import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRole;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import com.comunidad360.user.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BankingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AppUserRepository userRepository;

    @Autowired
    CommunityRepository communityRepository;

    @Autowired
    MembershipRepository membershipRepository;

    @Autowired
    MembershipRoleRepository membershipRoleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static final String CSV = """
            Fecha;Descripcion;Importe;Saldo;Referencia
            01/03/2026;Transferencia cuota;1.234,56;10.000,00;REF1
            02/03/2026;Gasto limpieza;-450,00;9.550,00;REF2
            03/03/2026;Cuota;100,00;9.650,00;REF3
            """;

    @Test
    void adminCanImportTransactionsAndDetectDuplicates() throws Exception {
        AppUser admin = createUser(uniqueEmail("bank-admin"));
        Community community = createCommunity("Bank Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());

        MvcResult accountResult = mockMvc.perform(post(bankingPath(community.getId()) + "/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Cuenta principal",
                                "bankName", "Banco Demo",
                                "iban", "ES9121000418450200051332"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ibanLast4").value("1332"))
                .andExpect(jsonPath("$.ibanMasked").value("**** **** **** 1332"))
                .andReturn();
        UUID accountId = UUID.fromString(json(accountResult).get("id").asText());

        mockMvc.perform(importRequest(community.getId(), token, accountId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rowCount").value(3))
                .andExpect(jsonPath("$.successCount").value(3))
                .andExpect(jsonPath("$.duplicateCount").value(0))
                .andExpect(jsonPath("$.errorCount").value(0));

        JsonNode transactions = transactions(token, community.getId());
        assertThat(transactions).hasSize(3);
        boolean foundTransfer = false;
        for (JsonNode transaction : transactions) {
            if ("Transferencia cuota".equals(transaction.get("description").asText())) {
                assertThat(new BigDecimal(transaction.get("amount").asText()))
                        .isEqualByComparingTo(new BigDecimal("1234.56"));
                foundTransfer = true;
            }
        }
        assertThat(foundTransfer).isTrue();

        mockMvc.perform(importRequest(community.getId(), token, accountId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.successCount").value(0))
                .andExpect(jsonPath("$.duplicateCount").value(3));

        JsonNode imports = json(mockMvc.perform(get(bankingPath(community.getId()) + "/imports")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(imports).hasSize(2);
        assertThat(transactions(token, community.getId())).hasSize(3);
    }

    @Test
    void adminCanReconcileWithLedgerEntryAndUndo() throws Exception {
        AppUser admin = createUser(uniqueEmail("bank-reconcile"));
        Community community = createCommunity("Reconciliation Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());
        UUID accountId = createBankAccount(token, community.getId());
        mockMvc.perform(importRequest(community.getId(), token, accountId))
                .andExpect(status().isCreated());

        UUID fiscalYearId = createFiscalYear(token, community.getId(), 2026);
        UUID ledgerId = createLedger(token, community.getId(), fiscalYearId, "INCOME", "Cuotas", "1234.56");
        mockMvc.perform(post("/api/v1/communities/" + community.getId()
                        + "/finance/ledger-entries/" + ledgerId + "/post")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode transactions = transactions(token, community.getId());
        UUID transactionId = null;
        for (JsonNode transaction : transactions) {
            if ("Transferencia cuota".equals(transaction.get("description").asText())) {
                transactionId = UUID.fromString(transaction.get("id").asText());
            }
        }
        assertThat(transactionId).isNotNull();

        MvcResult reconciliation = mockMvc.perform(post(bankingPath(community.getId())
                        + "/transactions/" + transactionId + "/reconcile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ledgerEntryId", ledgerId.toString(),
                                "matchedAmount", "1234.56"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledgerEntryId").value(ledgerId.toString()))
                .andReturn();
        UUID reconciliationId = UUID.fromString(json(reconciliation).get("id").asText());

        assertThat(transactionStatus(token, community.getId(), transactionId)).isEqualTo("RECONCILED");

        mockMvc.perform(delete(bankingPath(community.getId()) + "/reconciliations/" + reconciliationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(transactionStatus(token, community.getId(), transactionId)).isEqualTo("UNRECONCILED");
    }

    @Test
    void reconciliationRequiresMatchingAmount() throws Exception {
        AppUser admin = createUser(uniqueEmail("bank-amount"));
        Community community = createCommunity("Amount Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());
        UUID accountId = createBankAccount(token, community.getId());
        mockMvc.perform(importRequest(community.getId(), token, accountId))
                .andExpect(status().isCreated());
        JsonNode transactions = transactions(token, community.getId());
        UUID transactionId = UUID.fromString(transactions.get(0).get("id").asText());

        UUID fiscalYearId = createFiscalYear(token, community.getId(), 2026);
        UUID ledgerId = createLedger(token, community.getId(), fiscalYearId, "INCOME", "Cuotas", "10.00");
        mockMvc.perform(post("/api/v1/communities/" + community.getId()
                        + "/finance/ledger-entries/" + ledgerId + "/post")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post(bankingPath(community.getId())
                        + "/transactions/" + transactionId + "/reconcile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ledgerEntryId", ledgerId.toString(),
                                "matchedAmount", "10.00"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BANK_RECONCILIATION_AMOUNT_MISMATCH"));
    }

    @Test
    void ownerCannotImportAndCrossCommunityAccessIsRejected() throws Exception {
        AppUser admin = createUser(uniqueEmail("bank-owner-admin"));
        AppUser owner = createUser(uniqueEmail("bank-owner"));
        AppUser otherAdmin = createUser(uniqueEmail("bank-other"));
        Community community = createCommunity("Owner Bank Community");
        Community otherCommunity = createCommunity("Other Bank Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        createMembership(otherCommunity, otherAdmin, "COMMUNITY_ADMIN");
        String adminToken = accessToken(admin.getEmail());
        UUID accountId = createBankAccount(adminToken, community.getId());
        String ownerToken = accessToken(owner.getEmail());
        String otherToken = accessToken(otherAdmin.getEmail());

        mockMvc.perform(importRequest(community.getId(), ownerToken, accountId))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(bankingPath(community.getId()) + "/accounts")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.RequestBuilder importRequest(UUID communityId, String token,
                                                                            UUID accountId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "movimientos.csv",
                "text/csv",
                CSV.getBytes(StandardCharsets.UTF_8)
        );
        return multipart(bankingPath(communityId) + "/imports")
                .file(file)
                .header("Authorization", "Bearer " + token)
                .param("accountId", accountId.toString())
                .param("dateColumn", "Fecha")
                .param("descriptionColumn", "Descripcion")
                .param("amountColumn", "Importe")
                .param("balanceColumn", "Saldo")
                .param("externalIdColumn", "Referencia")
                .param("separator", ";");
    }

    private UUID createBankAccount(String token, UUID communityId) throws Exception {
        MvcResult result = mockMvc.perform(post(bankingPath(communityId) + "/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Cuenta",
                                "bankName", "Banco",
                                "iban", "ES9121000418450200051332"))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(result).get("id").asText());
    }

    private String transactionStatus(String token, UUID communityId, UUID transactionId) throws Exception {
        for (JsonNode transaction : transactions(token, communityId)) {
            if (transactionId.toString().equals(transaction.get("id").asText())) {
                return transaction.get("status").asText();
            }
        }
        return null;
    }

    private JsonNode transactions(String token, UUID communityId) throws Exception {
        return json(mockMvc.perform(get(bankingPath(communityId) + "/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
    }

    private UUID createFiscalYear(String token, UUID communityId, int year) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/communities/" + communityId + "/finance/fiscal-years")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "year", year,
                                "startDate", year + "-01-01",
                                "endDate", year + "-12-31"))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(result).get("id").asText());
    }

    private UUID createLedger(String token, UUID communityId, UUID fiscalYearId, String type,
                              String category, String amount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/communities/" + communityId
                        + "/finance/ledger-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fiscalYearId", fiscalYearId.toString(),
                                "entryDate", "2026-03-01",
                                "type", type,
                                "category", category,
                                "description", "Apunte banco",
                                "amount", amount))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(result).get("id").asText());
    }

    private String bankingPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/finance/banking";
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String accessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("accessToken").asText();
    }

    private AppUser createUser(String email) {
        Instant now = Instant.now();
        AppUser user = new AppUser(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode("Password123!"),
                "Bank Tester",
                "es",
                UserStatus.ACTIVE,
                now,
                now
        );
        user.setEmailVerifiedAt(now);
        return userRepository.save(user);
    }

    private Community createCommunity(String name) {
        Instant now = Instant.now();
        return communityRepository.save(new Community(
                UUID.randomUUID(),
                name,
                "Europe/Madrid",
                "es",
                "EUR",
                CommunityStatus.ACTIVE,
                now,
                now
        ));
    }

    private Membership createMembership(Community community, AppUser user, String roleCode) {
        Instant now = Instant.now();
        Membership membership = membershipRepository.save(new Membership(
                UUID.randomUUID(),
                community.getId(),
                user.getId(),
                MembershipStatus.ACTIVE,
                now,
                now,
                now
        ));
        membershipRoleRepository.save(new MembershipRole(
                UUID.randomUUID(),
                membership.getId(),
                roleCode,
                now,
                user.getId()
        ));
        return membership;
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}

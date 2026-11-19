package com.comunidad360.finance;

import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRole;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.property.Unit;
import com.comunidad360.property.UnitRepository;
import com.comunidad360.property.UnitType;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanceIntegrationTest {

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
    UnitRepository unitRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void assessmentAllocationAndPaymentFlow() throws Exception {
        AppUser admin = createUser(uniqueEmail("finance-admin"));
        Community community = createCommunity("Finance Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        UUID unitA = createUnit(community, "1A", "60.000000");
        UUID unitB = createUnit(community, "1B", "40.000000");
        String token = accessToken(admin.getEmail());
        UUID fiscalYearId = createFiscalYear(token, community.getId(), 2026);

        MvcResult assessmentResult = createAssessment(token, community.getId(), fiscalYearId, "100.00", "COEFFICIENT")
                .andExpect(status().isCreated())
                .andReturn();
        UUID assessmentId = UUID.fromString(json(assessmentResult).get("id").asText());

        mockMvc.perform(post(assessmentsPath(community.getId()) + "/" + assessmentId + "/issue")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.chargesCount").value(2));

        MvcResult chargesResult = mockMvc.perform(get(chargesPath(community.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode charges = json(chargesResult);
        assertThat(charges).hasSize(2);
        BigDecimal chargeSum = BigDecimal.ZERO;
        UUID chargeA = null;
        for (JsonNode charge : charges) {
            chargeSum = chargeSum.add(new BigDecimal(charge.get("amount").asText()));
            if ("1A".equals(charge.get("unitCode").asText())) {
                chargeA = UUID.fromString(charge.get("id").asText());
            }
        }
        assertThat(chargeSum).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(chargeA).isNotNull();

        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> paymentBody = new HashMap<>();
        paymentBody.put("assessmentChargeId", chargeA.toString());
        paymentBody.put("amount", "60.00");
        paymentBody.put("paymentDate", LocalDate.now().toString());
        paymentBody.put("method", "BANK_TRANSFER");
        MvcResult firstPayment = createPayment(token, community.getId(), paymentBody, idempotencyKey)
                .andExpect(status().isCreated())
                .andReturn();
        UUID paymentId = UUID.fromString(json(firstPayment).get("id").asText());
        MvcResult repeatedPayment = createPayment(token, community.getId(), paymentBody, idempotencyKey)
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(json(repeatedPayment).get("id").asText()).isEqualTo(paymentId.toString());

        JsonNode paidCharges = json(mockMvc.perform(get(chargesPath(community.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
        boolean paidFound = false;
        for (JsonNode charge : paidCharges) {
            if ("1A".equals(charge.get("unitCode").asText())) {
                assertThat(charge.get("status").asText()).isEqualTo("PAID");
                paidFound = true;
            }
        }
        assertThat(paidFound).isTrue();

        JsonNode summary = summary(token, community.getId());
        assertThat(new BigDecimal(summary.get("pendingCharges").asText()))
                .isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(summary.get("scope").asText()).isEqualTo("COMMUNITY");
        assertThat(summary.get("fiscalYear").asInt()).isEqualTo(2026);
    }

    @Test
    void equalAllocationDistributesRoundingCents() throws Exception {
        AppUser admin = createUser(uniqueEmail("finance-rounding"));
        Community community = createCommunity("Rounding Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createUnit(community, "1A", "33.333333");
        createUnit(community, "1B", "33.333333");
        createUnit(community, "1C", "33.333334");
        String token = accessToken(admin.getEmail());
        UUID fiscalYearId = createFiscalYear(token, community.getId(), 2026);

        MvcResult result = createAssessment(token, community.getId(), fiscalYearId, "100.00", "EQUAL")
                .andExpect(status().isCreated())
                .andReturn();
        UUID assessmentId = UUID.fromString(json(result).get("id").asText());
        mockMvc.perform(post(assessmentsPath(community.getId()) + "/" + assessmentId + "/issue")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode charges = json(mockMvc.perform(get(chargesPath(community.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
        BigDecimal sum = BigDecimal.ZERO;
        int count3340 = 0;
        int count3330 = 0;
        for (JsonNode charge : charges) {
            BigDecimal amount = new BigDecimal(charge.get("amount").asText());
            sum = sum.add(amount);
            if (amount.compareTo(new BigDecimal("33.34")) == 0) {
                count3340++;
            }
            if (amount.compareTo(new BigDecimal("33.33")) == 0) {
                count3330++;
            }
        }
        assertThat(sum).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(count3340).isEqualTo(1);
        assertThat(count3330).isEqualTo(2);
    }

    @Test
    void ledgerInvoicesAndBudgetFlow() throws Exception {
        AppUser admin = createUser(uniqueEmail("finance-ledger"));
        Community community = createCommunity("Ledger Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());
        UUID fiscalYearId = createFiscalYear(token, community.getId(), 2026);

        UUID incomeId = createLedger(token, community.getId(), fiscalYearId, "INCOME", "Cuotas", "200.00")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString().transform(this::uuidFromJson);
        UUID expenseId = createLedger(token, community.getId(), fiscalYearId, "EXPENSE", "Limpieza", "80.00")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString().transform(this::uuidFromJson);
        postLedger(token, community.getId(), incomeId).andExpect(status().isOk());
        postLedger(token, community.getId(), expenseId).andExpect(status().isOk());

        MvcResult invoiceResult = createInvoice(token, community.getId(), fiscalYearId,
                "Proveedor", "F-1", "100.00")
                .andExpect(status().isCreated())
                .andReturn();
        UUID invoiceId = UUID.fromString(json(invoiceResult).get("id").asText());
        JsonNode summary = summary(token, community.getId());
        assertThat(new BigDecimal(summary.get("balance").asText())).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(new BigDecimal(summary.get("income").asText())).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(new BigDecimal(summary.get("expense").asText())).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(new BigDecimal(summary.get("invoicesPending").asText())).isEqualByComparingTo(new BigDecimal("100.00"));

        Map<String, Object> invoicePayment = new HashMap<>();
        invoicePayment.put("invoiceId", invoiceId.toString());
        invoicePayment.put("amount", "50.00");
        invoicePayment.put("paymentDate", LocalDate.now().toString());
        invoicePayment.put("method", "BANK_TRANSFER");
        createPayment(token, community.getId(), invoicePayment, UUID.randomUUID().toString())
                .andExpect(status().isCreated());
        assertThat(new BigDecimal(summary(token, community.getId()).get("invoicesPending").asText()))
                .isEqualByComparingTo(new BigDecimal("50.00"));

        UUID budgetId = createBudget(token, community.getId(), fiscalYearId)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString().transform(this::uuidFromJson);
        addBudgetItem(token, community.getId(), budgetId, "INCOME", "Cuotas", "120.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value("120.00"));
        addBudgetItem(token, community.getId(), budgetId, "EXPENSE", "Limpieza", "60.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value("60.00"));
        mockMvc.perform(post(budgetsPath(community.getId()) + "/" + budgetId + "/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        addBudgetItem(token, community.getId(), budgetId, "INCOME", "Otra", "10.00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUDGET_NOT_EDITABLE"));
    }

    @Test
    void ownerSeesOnlyOwnChargesAndCannotAccessFinanceManagement() throws Exception {
        AppUser admin = createUser(uniqueEmail("finance-owner-admin"));
        AppUser owner = createUser(uniqueEmail("finance-owner"));
        Community community = createCommunity("Owner Finance Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        UUID unitA = createUnit(community, "1A", "60.000000");
        createUnit(community, "1B", "40.000000");
        String adminToken = accessToken(admin.getEmail());
        UUID fiscalYearId = createFiscalYear(adminToken, community.getId(), 2026);

        createOwnership(adminToken, community.getId(), unitA, owner.getId());

        MvcResult assessment = createAssessment(adminToken, community.getId(), fiscalYearId, "100.00", "COEFFICIENT")
                .andExpect(status().isCreated())
                .andReturn();
        UUID assessmentId = UUID.fromString(json(assessment).get("id").asText());
        mockMvc.perform(post(assessmentsPath(community.getId()) + "/" + assessmentId + "/issue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String ownerToken = accessToken(owner.getEmail());
        JsonNode charges = json(mockMvc.perform(get(chargesPath(community.getId()))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(charges).hasSize(1);
        assertThat(charges.get(0).get("unitCode").asText()).isEqualTo("1A");
        assertThat(new BigDecimal(charges.get(0).get("amount").asText()))
                .isEqualByComparingTo(new BigDecimal("60.00"));

        JsonNode ownerSummary = summary(ownerToken, community.getId());
        assertThat(ownerSummary.get("scope").asText()).isEqualTo("OWN");
        assertThat(new BigDecimal(ownerSummary.get("pendingCharges").asText()))
                .isEqualByComparingTo(new BigDecimal("60.00"));

        mockMvc.perform(get(assessmentsPath(community.getId()))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void closedFiscalYearRejectsWritesAndDraftEntriesBlockClosing() throws Exception {
        AppUser admin = createUser(uniqueEmail("finance-closed"));
        Community community = createCommunity("Closed Finance Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());
        UUID fiscalYearId = createFiscalYear(token, community.getId(), 2026);

        UUID entryId = createLedger(token, community.getId(), fiscalYearId, "INCOME", "Cuotas", "10.00")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString().transform(this::uuidFromJson);
        mockMvc.perform(post(fiscalYearsPath(community.getId()) + "/" + fiscalYearId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FISCAL_YEAR_HAS_DRAFT_ENTRIES"));

        postLedger(token, community.getId(), entryId).andExpect(status().isOk());
        mockMvc.perform(post(fiscalYearsPath(community.getId()) + "/" + fiscalYearId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        createAssessment(token, community.getId(), fiscalYearId, "50.00", "COEFFICIENT")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FISCAL_YEAR_CLOSED"));
        createLedger(token, community.getId(), fiscalYearId, "INCOME", "Otra", "5.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FISCAL_YEAR_CLOSED"));
    }

    @Test
    void crossCommunityFinanceAccessIsRejected() throws Exception {
        AppUser userA = createUser(uniqueEmail("finance-cross-a"));
        AppUser userB = createUser(uniqueEmail("finance-cross-b"));
        Community communityA = createCommunity("Finance Cross A");
        Community communityB = createCommunity("Finance Cross B");
        createMembership(communityA, userA, "COMMUNITY_ADMIN");
        createMembership(communityB, userB, "COMMUNITY_ADMIN");
        String tokenA = accessToken(userA.getEmail());

        mockMvc.perform(get("/api/v1/communities/" + communityB.getId() + "/finance/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    private UUID createFiscalYear(String token, UUID communityId, int year) throws Exception {
        MvcResult result = mockMvc.perform(post(fiscalYearsPath(communityId))
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

    private ResultActions createAssessment(String token, UUID communityId, UUID fiscalYearId,
                                           String totalAmount, String allocationMethod) throws Exception {
        return mockMvc.perform(post(assessmentsPath(communityId))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "fiscalYearId", fiscalYearId.toString(),
                        "concept", "Cuota 2026",
                        "type", "ORDINARY",
                        "periodStart", "2026-01-01",
                        "periodEnd", "2026-12-31",
                        "dueDate", "2026-06-30",
                        "totalAmount", totalAmount,
                        "allocationMethod", allocationMethod))));
    }

    private ResultActions createPayment(String token, UUID communityId, Map<String, Object> body,
                                        String idempotencyKey) throws Exception {
        return mockMvc.perform(post(paymentsPath(communityId))
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions createLedger(String token, UUID communityId, UUID fiscalYearId, String type,
                                       String category, String amount) throws Exception {
        return mockMvc.perform(post(ledgerPath(communityId))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "fiscalYearId", fiscalYearId.toString(),
                        "entryDate", "2026-03-15",
                        "type", type,
                        "category", category,
                        "description", "Apunte de prueba",
                        "amount", amount))));
    }

    private ResultActions postLedger(String token, UUID communityId, UUID entryId) throws Exception {
        return mockMvc.perform(post(ledgerPath(communityId) + "/" + entryId + "/post")
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions createInvoice(String token, UUID communityId, UUID fiscalYearId,
                                        String provider, String number, String total) throws Exception {
        return mockMvc.perform(post(invoicesPath(communityId))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "fiscalYearId", fiscalYearId.toString(),
                        "providerName", provider,
                        "number", number,
                        "issueDate", "2026-04-01",
                        "dueDate", "2026-05-01",
                        "subtotal", total,
                        "taxAmount", "0.00",
                        "total", total))));
    }

    private ResultActions createBudget(String token, UUID communityId, UUID fiscalYearId) throws Exception {
        return mockMvc.perform(post(budgetsPath(communityId))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "fiscalYearId", fiscalYearId.toString(),
                        "notes", "Presupuesto de prueba"))));
    }

    private ResultActions addBudgetItem(String token, UUID communityId, UUID budgetId, String type,
                                        String category, String amount) throws Exception {
        return mockMvc.perform(post(budgetsPath(communityId) + "/" + budgetId + "/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "type", type,
                        "category", category,
                        "description", "Partida de prueba",
                        "plannedAmount", amount))));
    }

    private ResultActions createOwnership(String token, UUID communityId, UUID unitId, UUID ownerId) throws Exception {
        return mockMvc.perform(post("/api/v1/communities/" + communityId + "/units/" + unitId + "/ownerships")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "userId", ownerId.toString(),
                        "ownershipType", "OWNER",
                        "sharePercentage", "100.0000",
                        "validFrom", LocalDate.now().toString()))))
                .andExpect(status().isCreated());
    }

    private JsonNode summary(String token, UUID communityId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/communities/" + communityId + "/finance/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return json(result);
    }

    private String fiscalYearsPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/finance/fiscal-years";
    }

    private String assessmentsPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/finance/assessments";
    }

    private String chargesPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/finance/charges";
    }

    private String paymentsPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/finance/payments";
    }

    private String ledgerPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/finance/ledger-entries";
    }

    private String invoicesPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/finance/invoices";
    }

    private String budgetsPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/finance/budgets";
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UUID uuidFromJson(String value) {
        try {
            return UUID.fromString(objectMapper.readTree(value).get("id").asText());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
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
                "Finance Tester",
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

    private UUID createUnit(Community community, String code, String coefficient) {
        Instant now = Instant.now();
        Unit unit = new Unit(
                UUID.randomUUID(),
                community.getId(),
                code,
                UnitType.VIVIENDA,
                new BigDecimal(coefficient),
                now,
                now
        );
        return unitRepository.save(unit).getId();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}

package com.comunidad360.documents;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentIntegrationTest {

    private static final byte[] PDF = "%PDF-1.4\nComunidad 360\n%%EOF".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PDF_V2 = "%PDF-1.7\nComunidad 360 v2\n%%EOF".getBytes(StandardCharsets.UTF_8);

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
    DocumentAccessLogRepository accessLogRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void communityBoardAndOwnersVisibilityIsEnforced() throws Exception {
        AppUser admin = createUser(uniqueEmail("doc-visibility-admin"));
        AppUser owner = createUser(uniqueEmail("doc-visibility-owner"));
        AppUser tenant = createUser(uniqueEmail("doc-visibility-tenant"));
        AppUser otherOwner = createUser(uniqueEmail("doc-visibility-other"));
        Community community = createCommunity("Document Visibility");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        createMembership(community, tenant, "TENANT");
        createMembership(community, otherOwner, "OWNER");
        UUID unitId = createUnit(community, "1A");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        String tenantToken = accessToken(tenant.getEmail());
        createOwnership(adminToken, community.getId(), unitId, owner.getId());

        UUID communityDocument = createDocument(adminToken, community.getId(),
                "Acta comunidad", "ACTA", "COMMUNITY", "acta.pdf", "application/pdf", PDF);

        list(ownerToken, community.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(communityDocument.toString()));
        list(tenantToken, community.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(communityDocument.toString()));

        UUID ownersDocument = createDocument(ownerToken, community.getId(),
                "Acta propietarios", "ACTA", "OWNERS", "propietarios.pdf", "application/pdf", PDF);
        list(tenantToken, community.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        list(ownerToken, community.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(ownersDocument.toString())));

        UUID boardDocument = createDocument(adminToken, community.getId(),
                "Acta junta", "ACTA", "BOARD", "junta.pdf", "application/pdf", PDF);
        list(ownerToken, community.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        list(adminToken, community.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].id", hasItem(boardDocument.toString())));

        mockMvc.perform(get(documentPath(community.getId(), boardDocument) + "/download")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(documentPath(community.getId(), ownersDocument) + "/download")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void versionsAreAppendedAndCurrentVersionChanges() throws Exception {
        AppUser admin = createUser(uniqueEmail("doc-version-admin"));
        Community community = createCommunity("Document Versions");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());

        UUID documentId = createDocument(token, community.getId(),
                "Presupuesto", "PRESUPUESTO", "COMMUNITY", "presupuesto.pdf", "application/pdf", PDF);
        JsonNode version = createVersion(token, community.getId(), documentId,
                "presupuesto-v2.pdf", "application/pdf", PDF_V2);
        assertThat(version.get("currentVersionNumber").asInt()).isEqualTo(2);

        JsonNode detail = detail(token, community.getId(), documentId);
        assertThat(detail.get("document").get("currentVersionNumber").asInt()).isEqualTo(2);
        assertThat(detail.get("versions")).hasSize(2);
        assertThat(detail.get("versions").get(0).get("versionNumber").asInt()).isEqualTo(2);
        UUID oldVersionId = UUID.fromString(detail.get("versions").get(1).get("id").asText());

        mockMvc.perform(get(documentPath(community.getId(), documentId) + "/download?versionId=" + oldVersionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PDF));

        long currentVersion = detail.get("document").get("version").asLong();
        mockMvc.perform(patch(documentPath(community.getId(), documentId))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Presupuesto 2026",
                                "version", currentVersion))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Presupuesto 2026"));

        mockMvc.perform(patch(documentPath(community.getId(), documentId))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Conflicto",
                                "version", currentVersion))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));

        assertThat(accessLogRepository.countByCommunityIdAndDocumentIdAndAction(
                community.getId(), documentId, DocumentAccessAction.UPLOAD)).isEqualTo(2);
        assertThat(accessLogRepository.countByCommunityIdAndDocumentIdAndAction(
                community.getId(), documentId, DocumentAccessAction.DOWNLOAD)).isEqualTo(1);
    }

    @Test
    void privateDocumentsAndUploadPermissionsAreEnforced() throws Exception {
        AppUser admin = createUser(uniqueEmail("doc-private-admin"));
        AppUser owner = createUser(uniqueEmail("doc-private-owner"));
        AppUser otherOwner = createUser(uniqueEmail("doc-private-other"));
        AppUser tenant = createUser(uniqueEmail("doc-private-tenant"));
        Community community = createCommunity("Document Private");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        createMembership(community, otherOwner, "OWNER");
        createMembership(community, tenant, "TENANT");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        String otherToken = accessToken(otherOwner.getEmail());
        String tenantToken = accessToken(tenant.getEmail());

        UUID documentId = createDocument(ownerToken, community.getId(),
                "Documento privado", "OTRO", "PRIVATE", "privado.pdf", "application/pdf", PDF);

        list(ownerToken, community.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(documentId.toString()));
        list(otherToken, community.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        list(adminToken, community.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(documentId.toString()));

        mockMvc.perform(get(documentPath(community.getId(), documentId))
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(documentPath(community.getId(), documentId) + "/download")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart(documentsPath(community.getId()))
                        .file(new MockMultipartFile("file", "nota.pdf", "application/pdf", PDF))
                        .param("title", "No permitido")
                        .param("category", "OTRO")
                        .param("visibility", "COMMUNITY")
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidFilesAndCrossCommunityAccessAreRejected() throws Exception {
        AppUser adminA = createUser(uniqueEmail("doc-cross-a"));
        AppUser adminB = createUser(uniqueEmail("doc-cross-b"));
        Community communityA = createCommunity("Document Cross A");
        Community communityB = createCommunity("Document Cross B");
        createMembership(communityA, adminA, "COMMUNITY_ADMIN");
        createMembership(communityB, adminB, "COMMUNITY_ADMIN");
        String tokenA = accessToken(adminA.getEmail());
        String tokenB = accessToken(adminB.getEmail());

        UUID documentId = createDocument(tokenA, communityA.getId(),
                "Contrato", "CONTRATO", "COMMUNITY", "contrato.pdf", "application/pdf", PDF);

        mockMvc.perform(get(documentPath(communityA.getId(), documentId))
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart(documentsPath(communityA.getId()))
                        .file(new MockMultipartFile("file", "malware.exe",
                                "application/octet-stream", new byte[]{0x4D, 0x5A, 0x00, 0x00}))
                        .param("title", "Archivo inválido")
                        .param("category", "OTRO")
                        .param("visibility", "COMMUNITY")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DOCUMENT_TYPE_UNSUPPORTED"));
    }

    private UUID createDocument(String token, UUID communityId, String title, String category,
                                String visibility, String fileName, String mimeType, byte[] content)
            throws Exception {
        MvcResult result = mockMvc.perform(multipart(documentsPath(communityId))
                        .file(new MockMultipartFile("file", fileName, mimeType, content))
                        .param("title", title)
                        .param("category", category)
                        .param("visibility", visibility)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentVersionNumber").value(1))
                .andExpect(jsonPath("$.downloadUrl").value(
                        org.hamcrest.Matchers.startsWith("/communities/")))
                .andReturn();
        return UUID.fromString(json(result).get("id").asText());
    }

    private JsonNode createVersion(String token, UUID communityId, UUID documentId,
                                   String fileName, String mimeType, byte[] content) throws Exception {
        return json(mockMvc.perform(multipart(documentPath(communityId, documentId) + "/versions")
                        .file(new MockMultipartFile("file", fileName, mimeType, content))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private JsonNode detail(String token, UUID communityId, UUID documentId) throws Exception {
        return json(mockMvc.perform(get(documentPath(communityId, documentId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
    }

    private ResultActions list(String token, UUID communityId) throws Exception {
        return mockMvc.perform(get(documentsPath(communityId))
                .header("Authorization", "Bearer " + token));
    }

    private String documentsPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/documents";
    }

    private String documentPath(UUID communityId, UUID documentId) {
        return documentsPath(communityId) + "/" + documentId;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String accessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
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
                "Document Tester",
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

    private UUID createUnit(Community community, String code) {
        Instant now = Instant.now();
        Unit unit = new Unit(
                UUID.randomUUID(),
                community.getId(),
                code,
                UnitType.VIVIENDA,
                new BigDecimal("100.000000"),
                now,
                now
        );
        return unitRepository.save(unit).getId();
    }

    private void createOwnership(String token, UUID communityId, UUID unitId, UUID ownerId) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/v1/communities/" + communityId + "/units/" + unitId + "/ownerships")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", ownerId.toString(),
                                "ownershipType", "OWNER",
                                "sharePercentage", "100.0000",
                                "validFrom", LocalDate.now().toString()))))
                .andExpect(status().isCreated());
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}

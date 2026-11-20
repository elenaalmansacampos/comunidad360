package com.comunidad360.community;

import com.comunidad360.audit.AuditService;
import com.comunidad360.community.dto.DashboardMonthResponse;
import com.comunidad360.community.dto.DashboardResponse;
import com.comunidad360.incidents.Incident;
import com.comunidad360.incidents.IncidentRepository;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.providers.Provider;
import com.comunidad360.providers.ProviderRepository;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExportService {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final DashboardService dashboardService;
    private final MembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final MembershipRoleRepository roleRepository;
    private final IncidentRepository incidentRepository;
    private final ProviderRepository providerRepository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public ExportService(DashboardService dashboardService,
                         MembershipRepository membershipRepository,
                         AppUserRepository userRepository,
                         MembershipRoleRepository roleRepository,
                         IncidentRepository incidentRepository,
                         ProviderRepository providerRepository,
                         TenantAccessService tenantAccessService,
                         AuditService auditService) {
        this.dashboardService = dashboardService;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.incidentRepository = incidentRepository;
        this.providerRepository = providerRepository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public byte[] monthlyCsv(UUID communityId, UUID userId, HttpServletRequest request) {
        requireReport(communityId, userId);
        DashboardResponse dashboard = dashboardService.dashboard(communityId, userId);
        StringBuilder csv = new StringBuilder("mes;ingresos;gastos;saldo\n");
        for (DashboardMonthResponse month : dashboard.monthlyFinance()) {
            BigDecimal income = scale(month.income());
            BigDecimal expense = scale(month.expense());
            csv.append(month.month()).append(';')
                    .append(income.toPlainString()).append(';')
                    .append(expense.toPlainString()).append(';')
                    .append(income.subtract(expense).toPlainString()).append('\n');
        }
        return toBytes(communityId, userId, "MONTHLY_CSV", csv, request);
    }

    @Transactional(readOnly = true)
    public byte[] membersCsv(UUID communityId, UUID userId, HttpServletRequest request) {
        requireReport(communityId, userId);
        StringBuilder csv = new StringBuilder("nombre;email;estado;roles\n");
        for (Membership membership : membershipRepository.findByCommunityIdOrderByCreatedAtAsc(communityId)) {
            AppUser user = userRepository.findById(membership.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            List<String> roles = new ArrayList<>(roleRepository.findRoleCodesByMembershipId(membership.getId()));
            csv.append(value(user.getFullName())).append(';')
                    .append(value(user.getEmail())).append(';')
                    .append(value(membership.getStatus().name())).append(';')
                    .append(value(String.join("|", roles))).append('\n');
        }
        return toBytes(communityId, userId, "MEMBERS_CSV", csv, request);
    }

    @Transactional(readOnly = true)
    public byte[] incidentsCsv(UUID communityId, UUID userId, HttpServletRequest request) {
        requireReport(communityId, userId);
        StringBuilder csv = new StringBuilder("titulo;categoria;prioridad;estado;creada\n");
        for (Incident incident : incidentRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)) {
            csv.append(value(incident.getTitle())).append(';')
                    .append(value(incident.getCategory())).append(';')
                    .append(value(incident.getPriority() == null ? null : incident.getPriority().name())).append(';')
                    .append(value(incident.getStatus() == null ? null : incident.getStatus().name())).append(';')
                    .append(value(incident.getCreatedAt() == null ? null : incident.getCreatedAt().toString()))
                    .append('\n');
        }
        return toBytes(communityId, userId, "INCIDENTS_CSV", csv, request);
    }

    @Transactional(readOnly = true)
    public byte[] providersCsv(UUID communityId, UUID userId, HttpServletRequest request) {
        requireReport(communityId, userId);
        StringBuilder csv = new StringBuilder("nombre;cif;categoria;email;telefono;estado\n");
        for (Provider provider : providerRepository.findByCommunityIdOrderByNameAsc(communityId)) {
            csv.append(value(provider.getName())).append(';')
                    .append(value(provider.getTaxId())).append(';')
                    .append(value(provider.getCategory())).append(';')
                    .append(value(provider.getEmail())).append(';')
                    .append(value(provider.getPhone())).append(';')
                    .append(value(provider.getStatus() == null ? null : provider.getStatus().name()))
                    .append('\n');
        }
        return toBytes(communityId, userId, "PROVIDERS_CSV", csv, request);
    }

    private void requireReport(UUID communityId, UUID userId) {
        tenantAccessService.requirePermission(communityId, userId, "REPORT_VIEW");
    }

    private byte[] toBytes(UUID communityId, UUID userId, String type, StringBuilder csv,
                           HttpServletRequest request) {
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[UTF8_BOM.length + content.length];
        System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
        System.arraycopy(content, 0, result, UTF8_BOM.length, content.length);
        auditService.record(communityId, userId, "USER", "REPORT_EXPORTED", "REPORT",
                communityId, null, Map.of("type", type, "bytes", result.length), request);
        return result;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String value(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(";") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}

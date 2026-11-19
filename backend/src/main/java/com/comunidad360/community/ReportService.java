package com.comunidad360.community;

import com.comunidad360.audit.AuditService;
import com.comunidad360.community.dto.DashboardResponse;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"));

    private final DashboardService dashboardService;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public ReportService(DashboardService dashboardService,
                         TenantAccessService tenantAccessService,
                         PermissionService permissionService,
                         AuditService auditService) {
        this.dashboardService = dashboardService;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    public byte[] summaryPdf(UUID communityId, UUID userId, HttpServletRequest request) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "REPORT_VIEW");
        DashboardResponse dashboard = dashboardService.dashboard(communityId, userId);
        byte[] pdf = render(dashboard);
        auditService.record(communityId, userId, "USER", "REPORT_EXPORTED", "REPORT",
                communityId, null, Map.of("type", "SUMMARY_PDF", "bytes", pdf.length), request);
        return pdf;
    }

    private byte[] render(DashboardResponse dashboard) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 780;
                y = line(content, PDType1Font.HELVETICA_BOLD, 18, 50, y,
                        "COMUNIDAD 360 - Informe de comunidad");
                y = line(content, PDType1Font.HELVETICA, 11, 50, y,
                        dashboard.community().name());
                y = line(content, PDType1Font.HELVETICA, 9, 50, y,
                        "Generado: " + DATE_FORMAT.format(Instant.now()) + " - Version 0.1.0");
                y -= 8;

                y = line(content, PDType1Font.HELVETICA_BOLD, 12, 50, y, "Resumen general");
                y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                        "Miembros activos: " + dashboard.activeMembers());
                y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                        "Incidencias abiertas: " + dashboard.openIncidents());
                y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                        "Roles: " + String.join(", ", dashboard.roles()));
                y -= 8;

                if (dashboard.finance() != null) {
                    y = line(content, PDType1Font.HELVETICA_BOLD, 12, 50, y, "Situacion financiera");
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                            "Ejercicio: " + dashboard.finance().fiscalYear()
                                    + " - Ambito: " + dashboard.finance().scope());
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                            "Ingresos: " + money(dashboard.finance().income()));
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                            "Gastos: " + money(dashboard.finance().expense()));
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                            "Saldo: " + money(dashboard.finance().balance()));
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                            "Cuotas pendientes: " + money(dashboard.finance().pendingCharges()));
                    y -= 8;
                }

                y = line(content, PDType1Font.HELVETICA_BOLD, 12, 50, y, "Proximas reuniones");
                if (dashboard.upcomingMeetings().isEmpty()) {
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y, "Sin reuniones convocadas.");
                } else {
                    for (var meeting : dashboard.upcomingMeetings()) {
                        y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                                "- " + meeting.title() + " - " + DATE_FORMAT.format(meeting.scheduledStart())
                                        + (meeting.location() == null ? "" : " - " + meeting.location()));
                    }
                }
                y -= 8;

                y = line(content, PDType1Font.HELVETICA_BOLD, 12, 50, y, "Ultimas incidencias");
                if (dashboard.recentIncidents().isEmpty()) {
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y, "Sin incidencias recientes.");
                } else {
                    for (var incident : dashboard.recentIncidents()) {
                        y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                                "- " + incident.title() + " - " + incident.status()
                                        + " - " + incident.priority());
                    }
                }

                line(content, PDType1Font.HELVETICA, 8, 50, 35,
                        "COMUNIDAD 360 - Documento generado automaticamente - v0.1.0");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot generate report", exception);
        }
    }

    private float line(PDPageContentStream content, PDType1Font font, float size, float x, float y,
                       String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - (size + 5);
    }

    private String money(BigDecimal value) {
        return value == null ? "-" : MONEY_FORMAT.format(value);
    }
}

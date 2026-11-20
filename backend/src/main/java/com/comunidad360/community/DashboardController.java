package com.comunidad360.community;

import com.comunidad360.community.dto.DashboardResponse;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final ExportService exportService;

    public DashboardController(DashboardService dashboardService, ReportService reportService,
                               ExportService exportService) {
        this.dashboardService = dashboardService;
        this.reportService = reportService;
        this.exportService = exportService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@PathVariable UUID communityId,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.dashboard(communityId, user.id());
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<byte[]> summaryReport(@PathVariable UUID communityId,
                                                @AuthenticationPrincipal AuthenticatedUser user,
                                                HttpServletRequest request) {
        byte[] pdf = reportService.summaryPdf(communityId, user.id(), request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("informe-comunidad.pdf", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/reports/monthly.csv")
    public ResponseEntity<byte[]> monthlyCsv(@PathVariable UUID communityId,
                                             @AuthenticationPrincipal AuthenticatedUser user,
                                             HttpServletRequest request) {
        return csv(exportService.monthlyCsv(communityId, user.id(), request), "evolucion-mensual.csv");
    }

    @GetMapping("/reports/members.csv")
    public ResponseEntity<byte[]> membersCsv(@PathVariable UUID communityId,
                                             @AuthenticationPrincipal AuthenticatedUser user,
                                             HttpServletRequest request) {
        return csv(exportService.membersCsv(communityId, user.id(), request), "miembros.csv");
    }

    @GetMapping("/reports/incidents.csv")
    public ResponseEntity<byte[]> incidentsCsv(@PathVariable UUID communityId,
                                               @AuthenticationPrincipal AuthenticatedUser user,
                                               HttpServletRequest request) {
        return csv(exportService.incidentsCsv(communityId, user.id(), request), "incidencias.csv");
    }

    @GetMapping("/reports/providers.csv")
    public ResponseEntity<byte[]> providersCsv(@PathVariable UUID communityId,
                                               @AuthenticationPrincipal AuthenticatedUser user,
                                               HttpServletRequest request) {
        return csv(exportService.providersCsv(communityId, user.id(), request), "proveedores.csv");
    }

    private ResponseEntity<byte[]> csv(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(content.length)
                .body(content);
    }
}

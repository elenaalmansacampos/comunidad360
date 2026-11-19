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

    public DashboardController(DashboardService dashboardService, ReportService reportService) {
        this.dashboardService = dashboardService;
        this.reportService = reportService;
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
}

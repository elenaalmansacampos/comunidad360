package com.comunidad360.finance;

import com.comunidad360.finance.dto.CategoryReportResponse;
import com.comunidad360.finance.dto.FinanceSummaryResponse;
import com.comunidad360.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/finance")
public class FinanceSummaryController {

    private final FinanceSummaryService summaryService;
    private final FinanceReportService reportService;

    public FinanceSummaryController(FinanceSummaryService summaryService,
                                    FinanceReportService reportService) {
        this.summaryService = summaryService;
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public FinanceSummaryResponse summary(@PathVariable UUID communityId,
                                          @RequestParam(required = false) UUID fiscalYearId,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        return summaryService.summary(communityId, user.id(), fiscalYearId);
    }

    @GetMapping("/reports/categories")
    public List<CategoryReportResponse> categoryReport(@PathVariable UUID communityId,
                                                       @RequestParam(required = false) UUID fiscalYearId,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return reportService.categorySummary(communityId, user.id(), fiscalYearId);
    }
}

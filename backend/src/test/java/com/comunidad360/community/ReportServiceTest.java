package com.comunidad360.community;

import com.comunidad360.community.dto.CommunitySummaryResponse;
import com.comunidad360.community.dto.DashboardResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest {

    @Test
    void englishReportUsesEnglishLabelsAndDates() throws Exception {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);

        ReportService service = new ReportService(null, null, null, null, null, messageSource);
        DashboardResponse dashboard = new DashboardResponse(
                new CommunitySummaryResponse(UUID.randomUUID(), "Sample community",
                        List.of("COMMUNITY_ADMIN"), "ACTIVE"),
                List.of("COMMUNITY_ADMIN"),
                3,
                1,
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                true
        );

        byte[] pdf = service.render(dashboard, Locale.ENGLISH);

        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Community report");
            assertThat(text).contains("Overview");
            assertThat(text).contains("Active members: 3");
            assertThat(text).contains("Upcoming meetings");
        }
    }
}

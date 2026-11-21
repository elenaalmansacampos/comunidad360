package com.comunidad360.community;

import com.comunidad360.audit.AuditService;
import com.comunidad360.community.dto.DashboardMonthResponse;
import com.comunidad360.community.dto.DashboardResponse;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportService {

    private final DashboardService dashboardService;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final AppUserRepository userRepository;
    private final MessageSource messageSource;

    public ReportService(DashboardService dashboardService,
                         TenantAccessService tenantAccessService,
                         PermissionService permissionService,
                         AuditService auditService,
                         AppUserRepository userRepository,
                         MessageSource messageSource) {
        this.dashboardService = dashboardService;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.messageSource = messageSource;
    }

    public byte[] summaryPdf(UUID communityId, UUID userId, HttpServletRequest request) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "REPORT_VIEW");
        Locale locale = locale(userId);
        DashboardResponse dashboard = dashboardService.dashboard(communityId, userId);
        byte[] pdf = render(dashboard, locale);
        auditService.record(communityId, userId, "USER", "REPORT_EXPORTED", "REPORT",
                communityId, null, Map.of("type", "SUMMARY_PDF", "locale", locale.toLanguageTag(),
                        "bytes", pdf.length), request);
        return pdf;
    }

    public byte[] renderForCommunity(UUID communityId, UUID actorUserId) {
        Locale locale = locale(actorUserId);
        DashboardResponse dashboard = dashboardService.dashboard(communityId, actorUserId);
        return render(dashboard, locale);
    }

    private Locale locale(UUID userId) {
        String language = userRepository.findById(userId)
                .map(AppUser::getPreferredLanguage)
                .filter(value -> value != null && !value.isBlank())
                .orElse("es");
        return Locale.forLanguageTag("en".equalsIgnoreCase(language) ? "en-GB" : language);
    }

    byte[] render(DashboardResponse dashboard, Locale locale) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault());
        NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(locale);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 780;
                y = line(content, PDType1Font.HELVETICA_BOLD, 18, 50, y,
                        text(locale, "report.title"));
                y = line(content, PDType1Font.HELVETICA, 11, 50, y, dashboard.community().name());
                y = line(content, PDType1Font.HELVETICA, 9, 50, y,
                        text(locale, "report.generated", dateFormat.format(Instant.now())));
                y -= 8;

                y = line(content, PDType1Font.HELVETICA_BOLD, 12, 50, y, text(locale, "report.overview"));
                y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                        text(locale, "report.activeMembers", dashboard.activeMembers()));
                y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                        text(locale, "report.openIncidents", dashboard.openIncidents()));
                y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                        text(locale, "report.roles", String.join(", ", dashboard.roles())));
                y -= 8;

                if (dashboard.finance() != null) {
                    y = line(content, PDType1Font.HELVETICA_BOLD, 12, 50, y, text(locale, "report.finance"));
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                            text(locale, "report.fiscalYear", dashboard.finance().fiscalYear(),
                                    scope(locale, dashboard.finance().scope())));
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                            text(locale, "report.income", money(dashboard.finance().income(), moneyFormat)));
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                            text(locale, "report.expense", money(dashboard.finance().expense(), moneyFormat)));
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                            text(locale, "report.balance", money(dashboard.finance().balance(), moneyFormat)));
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                            text(locale, "report.pendingCharges", money(dashboard.finance().pendingCharges(), moneyFormat)));
                    y -= 8;
                }

                if (!dashboard.monthlyFinance().isEmpty()) {
                    y = chart(content, dashboard.monthlyFinance(), locale, y);
                }

                y = line(content, PDType1Font.HELVETICA_BOLD, 12, 50, y, text(locale, "report.upcomingMeetings"));
                if (dashboard.upcomingMeetings().isEmpty()) {
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y, text(locale, "report.noMeetings"));
                } else {
                    for (var meeting : dashboard.upcomingMeetings()) {
                        y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                                "- " + meeting.title() + " - " + dateFormat.format(meeting.scheduledStart())
                                        + (meeting.location() == null ? "" : " - " + meeting.location()));
                    }
                }
                y -= 8;

                y = line(content, PDType1Font.HELVETICA_BOLD, 12, 50, y, text(locale, "report.recentIncidents"));
                if (dashboard.recentIncidents().isEmpty()) {
                    y = line(content, PDType1Font.HELVETICA, 10, 50, y, text(locale, "report.noIncidents"));
                } else {
                    for (var incident : dashboard.recentIncidents()) {
                        y = line(content, PDType1Font.HELVETICA, 10, 50, y,
                                "- " + incident.title() + " - " + incident.status()
                                        + " - " + incident.priority());
                    }
                }

                line(content, PDType1Font.HELVETICA, 8, 50, 35, text(locale, "report.footer"));
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot generate report", exception);
        }
    }

    private float chart(PDPageContentStream content, List<DashboardMonthResponse> monthly,
                        Locale locale, float y) throws IOException {
        y = line(content, PDType1Font.HELVETICA_BOLD, 12, 50, y, text(locale, "report.monthlyChart"));
        float max = 0;
        for (DashboardMonthResponse month : monthly) {
            max = Math.max(max, Math.max(toFloat(month.income()), toFloat(month.expense())));
        }
        if (max <= 0) {
            return line(content, PDType1Font.HELVETICA, 10, 50, y, text(locale, "report.noMovements"));
        }
        float chartHeight = 90;
        float baseY = y - chartHeight - 10;
        float barWidth = 12;
        float x = 50;
        for (DashboardMonthResponse month : monthly) {
            float incomeHeight = chartHeight * toFloat(month.income()) / max;
            float expenseHeight = chartHeight * toFloat(month.expense()) / max;
            content.setNonStrokingColor(new java.awt.Color(0x2E7D32));
            content.addRect(x, baseY, barWidth, incomeHeight);
            content.fill();
            content.setNonStrokingColor(new java.awt.Color(0xC62828));
            content.addRect(x + barWidth + 2, baseY, barWidth, expenseHeight);
            content.fill();
            x += (barWidth * 2) + 6;
        }
        content.setStrokingColor(java.awt.Color.GRAY);
        content.moveTo(50, baseY);
        content.lineTo(x, baseY);
        content.stroke();
        content.setNonStrokingColor(java.awt.Color.BLACK);
        content.setStrokingColor(java.awt.Color.BLACK);
        return baseY - 14;
    }

    private float line(PDPageContentStream content, PDType1Font font, float size, float x, float y,
                       String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(sanitize(text));
        content.endText();
        return y - (size + 5);
    }

    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        String value = text
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ')
                .replace('\u2009', ' ')
                .replace('\u2011', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replace("\u2026", "...");
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character <= 0xFF || "€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸ".indexOf(character) >= 0) {
                result.append(character);
            } else {
                result.append('?');
            }
        }
        return result.toString();
    }

    private String text(Locale locale, String key, Object... args) {
        return messageSource.getMessage(key, args.length == 0 ? null : args, key, locale);
    }

    private String scope(Locale locale, String scope) {
        return messageSource.getMessage("report.scope." + scope, null, scope, locale);
    }

    private String money(BigDecimal value, NumberFormat format) {
        return value == null ? "-" : format.format(value);
    }

    private float toFloat(BigDecimal value) {
        return value == null ? 0f : value.floatValue();
    }
}

package com.comunidad360.community;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.community.dto.CommunitySummaryResponse;
import com.comunidad360.community.dto.DashboardComparisonResponse;
import com.comunidad360.community.dto.DashboardFinanceResponse;
import com.comunidad360.community.dto.DashboardIncidentResponse;
import com.comunidad360.community.dto.DashboardMeetingResponse;
import com.comunidad360.community.dto.DashboardMonthResponse;
import com.comunidad360.community.dto.DashboardResponse;
import com.comunidad360.finance.FinanceSummaryService;
import com.comunidad360.finance.FiscalYearRepository;
import com.comunidad360.finance.LedgerEntryRepository;
import com.comunidad360.finance.dto.FinanceSummaryResponse;
import com.comunidad360.incidents.IncidentService;
import com.comunidad360.incidents.IncidentStatus;
import com.comunidad360.incidents.dto.IncidentResponse;
import com.comunidad360.meetings.MeetingRepository;
import com.comunidad360.meetings.MeetingStatus;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DashboardService {

    private static final Set<IncidentStatus> OPEN_STATUSES = Set.of(
            IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS, IncidentStatus.WAITING_PROVIDER
    );

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final FinanceSummaryService financeSummaryService;
    private final FiscalYearRepository fiscalYearRepository;
    private final IncidentService incidentService;
    private final MeetingRepository meetingRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public DashboardService(CommunityRepository communityRepository,
                            MembershipRepository membershipRepository,
                            MembershipRoleRepository membershipRoleRepository,
                            TenantAccessService tenantAccessService,
                            PermissionService permissionService,
                            FinanceSummaryService financeSummaryService,
                            FiscalYearRepository fiscalYearRepository,
                            IncidentService incidentService,
                            MeetingRepository meetingRepository,
                            LedgerEntryRepository ledgerEntryRepository) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.financeSummaryService = financeSummaryService;
        this.fiscalYearRepository = fiscalYearRepository;
        this.incidentService = incidentService;
        this.meetingRepository = meetingRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID communityId, UUID userId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "COMMUNITY_NOT_FOUND", "errors.community.notFound"));
        List<String> roles = membershipRoleRepository.findRoleCodesByMembershipId(membership.getId());
        long activeMembers = membershipRepository.countByCommunityIdAndStatus(
                communityId, MembershipStatus.ACTIVE);

        List<IncidentResponse> incidents = List.of();
        if (permissionService.hasPermission(membership, "INCIDENT_VIEW_ALL")
                || permissionService.hasPermission(membership, "INCIDENT_VIEW_OWN")) {
            try {
                incidents = incidentService.list(communityId, userId, null, null, false);
            } catch (BusinessException ignored) {
                incidents = List.of();
            }
        }
        List<DashboardIncidentResponse> recentIncidents = incidents.stream()
                .limit(5)
                .map(incident -> new DashboardIncidentResponse(
                        incident.id(),
                        incident.title(),
                        incident.status(),
                        incident.priority(),
                        incident.unitCode(),
                        incident.createdAt()
                ))
                .toList();
        long openIncidents = incidents.stream()
                .filter(incident -> OPEN_STATUSES.contains(incident.status()))
                .count();

        List<DashboardMeetingResponse> upcomingMeetings = List.of();
        if (permissionService.hasPermission(membership, "MEETING_VIEW")) {
            upcomingMeetings = meetingRepository
                    .findTop5ByCommunityIdAndStatusAndScheduledStartAfterOrderByScheduledStartAsc(
                            communityId, MeetingStatus.CONVENED, Instant.now()).stream()
                    .map(meeting -> new DashboardMeetingResponse(
                            meeting.getId(),
                            meeting.getTitle(),
                            meeting.getStatus(),
                            meeting.getScheduledStart(),
                            meeting.getLocation()
                    ))
                    .toList();
        }

        FinanceSummaryResponse summary = financeSummary(membership, communityId, userId);
        DashboardFinanceResponse finance = summary == null ? null : new DashboardFinanceResponse(
                summary.fiscalYear(),
                summary.scope(),
                summary.currency(),
                money(summary.balance()),
                money(summary.income()),
                money(summary.expense()),
                money(summary.pendingCharges()),
                money(summary.invoicesPending())
        );
        List<DashboardMonthResponse> monthly = new ArrayList<>();
        if (summary != null && "COMMUNITY".equals(summary.scope())) {
            monthly = monthlyFinance(communityId, summary.fiscalYearId());
        }

        return new DashboardResponse(
                new CommunitySummaryResponse(community.getId(), community.getName(), roles,
                        community.getStatus().name()),
                roles,
                activeMembers,
                openIncidents,
                finance,
                recentIncidents,
                upcomingMeetings,
                monthly,
                comparison(monthly),
                permissionService.hasPermission(membership, "REPORT_VIEW")
        );
    }

    private FinanceSummaryResponse financeSummary(Membership membership, UUID communityId, UUID userId) {
        if (!permissionService.hasPermission(membership, "FINANCE_VIEW")
                || !fiscalYearRepository.existsByCommunityId(communityId)) {
            return null;
        }
        return financeSummaryService.summary(communityId, userId, null);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private DashboardComparisonResponse comparison(List<DashboardMonthResponse> monthly) {
        if (monthly.isEmpty()) {
            return null;
        }
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        DashboardMonthResponse current = monthly.get(currentMonth - 1);
        DashboardMonthResponse previous = currentMonth > 1 ? monthly.get(currentMonth - 2) : null;
        BigDecimal currentIncome = money(current.income());
        BigDecimal currentExpense = money(current.expense());
        if (previous == null) {
            return new DashboardComparisonResponse(currentMonth, 0, currentIncome, currentExpense,
                    null, null, null, null);
        }
        BigDecimal previousIncome = money(previous.income());
        BigDecimal previousExpense = money(previous.expense());
        return new DashboardComparisonResponse(
                currentMonth,
                currentMonth - 1,
                currentIncome,
                currentExpense,
                previousIncome,
                previousExpense,
                currentIncome.subtract(previousIncome),
                currentExpense.subtract(previousExpense)
        );
    }

    private List<DashboardMonthResponse> monthlyFinance(UUID communityId, UUID fiscalYearId) {
        BigDecimal[] income = new BigDecimal[13];
        BigDecimal[] expense = new BigDecimal[13];
        for (int month = 1; month <= 12; month++) {
            income[month] = BigDecimal.ZERO;
            expense[month] = BigDecimal.ZERO;
        }
        for (Object[] row : ledgerEntryRepository.sumMonthly(communityId, fiscalYearId)) {
            int month = ((Number) row[0]).intValue();
            String type = String.valueOf(row[1]);
            BigDecimal amount = row[2] instanceof BigDecimal value ? value : new BigDecimal(row[2].toString());
            if (month >= 1 && month <= 12) {
                if ("INCOME".equals(type)) {
                    income[month] = amount;
                } else if ("EXPENSE".equals(type)) {
                    expense[month] = amount;
                }
            }
        }
        List<DashboardMonthResponse> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            result.add(new DashboardMonthResponse(month, money(income[month]), money(expense[month])));
        }
        return result;
    }
}

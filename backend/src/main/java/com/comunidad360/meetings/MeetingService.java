package com.comunidad360.meetings;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.documents.Document;
import com.comunidad360.documents.DocumentRepository;
import com.comunidad360.meetings.dto.AgendaItemRequest;
import com.comunidad360.meetings.dto.AgendaItemResponse;
import com.comunidad360.meetings.dto.AgreementRequest;
import com.comunidad360.meetings.dto.AgreementResponse;
import com.comunidad360.meetings.dto.AttendanceRequest;
import com.comunidad360.meetings.dto.AttendanceResponse;
import com.comunidad360.meetings.dto.CreateMeetingRequest;
import com.comunidad360.meetings.dto.MeetingDetailResponse;
import com.comunidad360.meetings.dto.MeetingResponse;
import com.comunidad360.meetings.dto.UpdateMeetingRequest;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.notifications.NotificationService;
import com.comunidad360.property.PropertyOwnership;
import com.comunidad360.property.PropertyOwnershipRepository;
import com.comunidad360.property.Unit;
import com.comunidad360.property.UnitRepository;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingAgendaItemRepository agendaRepository;
    private final MeetingAttendanceRepository attendanceRepository;
    private final MeetingAgreementRepository agreementRepository;
    private final DocumentRepository documentRepository;
    private final MembershipRepository membershipRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final PropertyOwnershipRepository ownershipRepository;
    private final UnitRepository unitRepository;
    private final AppUserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public MeetingService(MeetingRepository meetingRepository,
                          MeetingAgendaItemRepository agendaRepository,
                          MeetingAttendanceRepository attendanceRepository,
                          MeetingAgreementRepository agreementRepository,
                          DocumentRepository documentRepository,
                          MembershipRepository membershipRepository,
                          TenantAccessService tenantAccessService,
                          PermissionService permissionService,
                          PropertyOwnershipRepository ownershipRepository,
                          UnitRepository unitRepository,
                          AppUserRepository userRepository,
                          NotificationService notificationService,
                          AuditService auditService) {
        this.meetingRepository = meetingRepository;
        this.agendaRepository = agendaRepository;
        this.attendanceRepository = attendanceRepository;
        this.agreementRepository = agreementRepository;
        this.documentRepository = documentRepository;
        this.membershipRepository = membershipRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.ownershipRepository = ownershipRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> list(UUID communityId, UUID userId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "MEETING_VIEW");
        return meetingRepository.findByCommunityIdOrderByScheduledStartDesc(communityId).stream()
                .map(MeetingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MeetingDetailResponse detail(UUID communityId, UUID userId, UUID meetingId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "MEETING_VIEW");
        Meeting meeting = find(communityId, meetingId);
        List<AgendaItemResponse> agenda = agendaRepository
                .findByCommunityIdAndMeetingIdOrderByPositionAsc(communityId, meetingId).stream()
                .map(AgendaItemResponse::from)
                .toList();
        List<AttendanceResponse> attendance = attendanceRepository
                .findByCommunityIdAndMeetingIdOrderByRecordedAtAsc(communityId, meetingId).stream()
                .map(this::toAttendanceResponse)
                .toList();
        List<AgreementResponse> agreements = agreementRepository
                .findByCommunityIdAndMeetingIdOrderByCreatedAtAsc(communityId, meetingId).stream()
                .map(AgreementResponse::from)
                .toList();
        return new MeetingDetailResponse(MeetingResponse.from(meeting), agenda, attendance, agreements);
    }

    @Transactional
    public MeetingResponse create(UUID communityId, UUID userId, CreateMeetingRequest request,
                                  HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "MEETING_MANAGE");
        validateDates(request.scheduledStart(), request.scheduledEnd());
        validateDocument(communityId, request.convocationDocumentId());
        MeetingStatus status = request.status() == null ? MeetingStatus.DRAFT : request.status();
        if (status != MeetingStatus.DRAFT && status != MeetingStatus.CONVENED) {
            throw BusinessException.unprocessable("MEETING_STATUS_INVALID", "errors.meeting.statusInvalid");
        }
        Instant now = Instant.now();
        Meeting meeting = meetingRepository.saveAndFlush(new Meeting(
                UUID.randomUUID(),
                communityId,
                request.title().trim(),
                request.type(),
                request.scheduledStart(),
                request.scheduledEnd(),
                clean(request.location()),
                status,
                userId,
                now,
                now
        ));
        if (request.convocationDocumentId() != null) {
            meeting.setConvocationDocumentId(request.convocationDocumentId());
            meeting = meetingRepository.saveAndFlush(meeting);
        }
        if (status == MeetingStatus.CONVENED) {
            notificationService.notifyMeetingConvened(meeting, userId);
        }
        auditService.record(communityId, userId, "USER", "MEETING_CREATED", "MEETING",
                meeting.getId(), null, MeetingResponse.from(meeting), httpRequest);
        return MeetingResponse.from(meeting);
    }

    @Transactional
    public MeetingResponse update(UUID communityId, UUID userId, UUID meetingId,
                                  UpdateMeetingRequest request, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "MEETING_MANAGE");
        Meeting meeting = find(communityId, meetingId);
        if (request.version() != null && request.version() != meeting.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        MeetingResponse before = MeetingResponse.from(meeting);
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            meeting.setTitle(request.title().trim());
        }
        if (request.type() != null) {
            meeting.setType(request.type());
        }
        if (request.scheduledStart() != null) {
            meeting.setScheduledStart(request.scheduledStart());
        }
        if (request.scheduledEnd() != null) {
            meeting.setScheduledEnd(request.scheduledEnd());
        }
        if (request.location() != null) {
            meeting.setLocation(clean(request.location()));
        }
        if (request.convocationDocumentId() != null) {
            validateDocument(communityId, request.convocationDocumentId());
            meeting.setConvocationDocumentId(request.convocationDocumentId());
        }
        if (request.minutesDocumentId() != null) {
            validateDocument(communityId, request.minutesDocumentId());
            meeting.setMinutesDocumentId(request.minutesDocumentId());
        }
        validateDates(meeting.getScheduledStart(), meeting.getScheduledEnd());
        MeetingStatus previousStatus = meeting.getStatus();
        if (request.status() != null && request.status() != previousStatus) {
            validateTransition(previousStatus, request.status());
            meeting.setStatus(request.status());
        }
        meeting.setUpdatedAt(Instant.now());
        Meeting saved = meetingRepository.saveAndFlush(meeting);
        if (previousStatus != MeetingStatus.CONVENED && saved.getStatus() == MeetingStatus.CONVENED) {
            notificationService.notifyMeetingConvened(saved, userId);
        }
        auditService.record(communityId, userId, "USER", "MEETING_UPDATED", "MEETING",
                meetingId, before, MeetingResponse.from(saved), httpRequest);
        return MeetingResponse.from(saved);
    }

    @Transactional
    public AgendaItemResponse addAgendaItem(UUID communityId, UUID userId, UUID meetingId,
                                            AgendaItemRequest request, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "MEETING_MANAGE");
        Meeting meeting = find(communityId, meetingId);
        if (meeting.getStatus() == MeetingStatus.HELD || meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw BusinessException.unprocessable("MEETING_NOT_EDITABLE", "errors.meeting.notEditable");
        }
        int position = request.position() == null
                ? (int) agendaRepository.countByCommunityIdAndMeetingId(communityId, meetingId) + 1
                : request.position();
        if (position < 1) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
        }
        MeetingAgendaItem item = agendaRepository.saveAndFlush(new MeetingAgendaItem(
                UUID.randomUUID(),
                communityId,
                meetingId,
                position,
                request.title().trim(),
                clean(request.description()),
                Instant.now()
        ));
        auditService.record(communityId, userId, "USER", "MEETING_AGENDA_ADDED", "MEETING",
                meetingId, null, AgendaItemResponse.from(item), httpRequest);
        return AgendaItemResponse.from(item);
    }

    @Transactional
    public AttendanceResponse recordAttendance(UUID communityId, UUID userId, UUID meetingId,
                                               AttendanceRequest request, HttpServletRequest httpRequest) {
        Membership manager = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(manager, "MEETING_MANAGE");
        Meeting meeting = find(communityId, meetingId);
        if (meeting.getStatus() != MeetingStatus.CONVENED && meeting.getStatus() != MeetingStatus.HELD) {
            throw BusinessException.unprocessable("MEETING_NOT_OPEN", "errors.meeting.notOpen");
        }
        Membership member = membershipRepository.findById(request.membershipId())
                .filter(value -> value.getCommunityId().equals(communityId))
                .filter(value -> value.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.unprocessable(
                        "MEETING_MEMBER_INVALID", "errors.meeting.memberInvalid"));
        if (request.attendanceType() == AttendanceType.REPRESENTED
                && request.representedByMembershipId() == null) {
            throw BusinessException.unprocessable("MEETING_REPRESENTATIVE_REQUIRED",
                    "errors.meeting.representativeRequired");
        }
        if (request.representedByMembershipId() != null
                && request.representedByMembershipId().equals(member.getId())) {
            throw BusinessException.unprocessable("MEETING_REPRESENTATIVE_INVALID",
                    "errors.meeting.representativeInvalid");
        }
        if (request.representedByMembershipId() != null) {
            membershipRepository.findById(request.representedByMembershipId())
                    .filter(value -> value.getCommunityId().equals(communityId))
                    .filter(value -> value.getStatus() == MembershipStatus.ACTIVE)
                    .orElseThrow(() -> BusinessException.unprocessable(
                            "MEETING_REPRESENTATIVE_INVALID", "errors.meeting.representativeInvalid"));
        }
        BigDecimal coefficient = coefficientFor(communityId, member.getUserId());
        MeetingAttendance attendance = attendanceRepository
                .findByMeetingIdAndMembershipId(meetingId, member.getId())
                .orElseGet(() -> new MeetingAttendance(
                        UUID.randomUUID(), communityId, meetingId, member.getId(),
                        request.attendanceType(), request.representedByMembershipId(),
                        coefficient, Instant.now()));
        attendance.setAttendanceType(request.attendanceType());
        attendance.setRepresentedByMembershipId(
                request.attendanceType() == AttendanceType.REPRESENTED
                        ? request.representedByMembershipId() : null);
        attendance.setCoefficientSnapshot(coefficient);
        attendance.setRecordedAt(Instant.now());
        MeetingAttendance saved = attendanceRepository.saveAndFlush(attendance);
        auditService.record(communityId, userId, "USER", "MEETING_ATTENDANCE_RECORDED", "MEETING",
                meetingId, null, toAttendanceResponse(saved), httpRequest);
        return toAttendanceResponse(saved);
    }

    @Transactional
    public AgreementResponse addAgreement(UUID communityId, UUID userId, UUID meetingId,
                                          AgreementRequest request, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "MEETING_MANAGE");
        find(communityId, meetingId);
        if (request.agendaItemId() != null) {
            agendaRepository.findByIdAndCommunityId(request.agendaItemId(), communityId)
                    .filter(item -> item.getMeetingId().equals(meetingId))
                    .orElseThrow(() -> BusinessException.unprocessable(
                            "MEETING_AGENDA_INVALID", "errors.meeting.agendaInvalid"));
        }
        BigDecimal coefficientFor = nonNegative(request.coefficientFor());
        BigDecimal coefficientAgainst = nonNegative(request.coefficientAgainst());
        BigDecimal coefficientAbstention = nonNegative(request.coefficientAbstention());
        boolean adopted = request.adopted() != null
                ? request.adopted()
                : request.votesFor() > request.votesAgainst();
        MeetingAgreement agreement = agreementRepository.saveAndFlush(new MeetingAgreement(
                UUID.randomUUID(),
                communityId,
                meetingId,
                request.agendaItemId(),
                request.title().trim(),
                clean(request.description()),
                request.votesFor(),
                request.votesAgainst(),
                request.abstentions(),
                coefficientFor,
                coefficientAgainst,
                coefficientAbstention,
                adopted,
                Instant.now()
        ));
        auditService.record(communityId, userId, "USER", "MEETING_AGREEMENT_ADDED", "MEETING",
                meetingId, null, AgreementResponse.from(agreement), httpRequest);
        return AgreementResponse.from(agreement);
    }

    @Transactional
    public MeetingResponse linkMinutes(UUID communityId, UUID userId, UUID meetingId, UUID documentId,
                                       HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "MEETING_MANAGE");
        Meeting meeting = find(communityId, meetingId);
        validateDocument(communityId, documentId);
        MeetingResponse before = MeetingResponse.from(meeting);
        meeting.setMinutesDocumentId(documentId);
        if (meeting.getStatus() == MeetingStatus.CONVENED) {
            meeting.setStatus(MeetingStatus.HELD);
        }
        meeting.setUpdatedAt(Instant.now());
        Meeting saved = meetingRepository.saveAndFlush(meeting);
        notificationService.notifyMeetingMinutes(saved, userId);
        auditService.record(communityId, userId, "USER", "MEETING_MINUTES_LINKED", "MEETING",
                meetingId, before, MeetingResponse.from(saved), httpRequest);
        return MeetingResponse.from(saved);
    }

    private Meeting find(UUID communityId, UUID meetingId) {
        return meetingRepository.findByIdAndCommunityId(meetingId, communityId)
                .orElseThrow(() -> BusinessException.notFound("MEETING_NOT_FOUND", "errors.resource.notFound"));
    }

    private void validateDocument(UUID communityId, UUID documentId) {
        if (documentId == null) {
            return;
        }
        documentRepository.findByIdAndCommunityId(documentId, communityId)
                .orElseThrow(() -> BusinessException.unprocessable(
                        "MEETING_DOCUMENT_INVALID", "errors.meeting.documentInvalid"));
    }

    private void validateDates(Instant start, Instant end) {
        if (start == null || (end != null && end.isBefore(start))) {
            throw BusinessException.unprocessable("MEETING_DATES_INVALID", "errors.meeting.datesInvalid");
        }
    }

    private void validateTransition(MeetingStatus from, MeetingStatus to) {
        boolean allowed = switch (from) {
            case DRAFT -> to == MeetingStatus.CONVENED || to == MeetingStatus.CANCELLED;
            case CONVENED -> to == MeetingStatus.HELD || to == MeetingStatus.CANCELLED;
            case HELD, CANCELLED -> false;
        };
        if (!allowed) {
            throw BusinessException.unprocessable("MEETING_STATUS_INVALID", "errors.meeting.statusInvalid");
        }
    }

    private BigDecimal coefficientFor(UUID communityId, UUID userId) {
        LocalDate today = LocalDate.now();
        BigDecimal total = BigDecimal.ZERO;
        for (PropertyOwnership ownership : ownershipRepository
                .findByCommunityIdAndUserIdOrderByValidFromDesc(communityId, userId)) {
            if (!ownership.isActiveOn(today)) {
                continue;
            }
            Unit unit = unitRepository.findById(ownership.getUnitId()).orElse(null);
            if (unit == null || !unit.getCommunityId().equals(communityId)) {
                continue;
            }
            total = total.add(unit.getCoefficient()
                    .multiply(ownership.getSharePercentage())
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        }
        return total.setScale(6, RoundingMode.HALF_UP);
    }

    private AttendanceResponse toAttendanceResponse(MeetingAttendance attendance) {
        Membership member = membershipRepository.findById(attendance.getMembershipId()).orElse(null);
        String memberName = member == null ? null : userName(member.getUserId());
        String representativeName = attendance.getRepresentedByMembershipId() == null
                ? null
                : membershipRepository.findById(attendance.getRepresentedByMembershipId())
                        .map(value -> userName(value.getUserId()))
                        .orElse(null);
        return AttendanceResponse.from(attendance, memberName, representativeName);
    }

    private String userName(UUID userId) {
        return userRepository.findById(userId)
                .map(this::displayName)
                .orElse(null);
    }

    private String displayName(AppUser user) {
        return user.getFullName() == null || user.getFullName().isBlank()
                ? user.getEmail()
                : user.getFullName();
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        if (value.signum() < 0) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

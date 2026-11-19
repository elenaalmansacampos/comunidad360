package com.comunidad360.incidents;

import com.comunidad360.audit.AuditService;
import com.comunidad360.building.Building;
import com.comunidad360.building.BuildingRepository;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.config.AppProperties;
import com.comunidad360.incidents.dto.CreateIncidentRequest;
import com.comunidad360.incidents.dto.IncidentAttachmentResponse;
import com.comunidad360.incidents.dto.IncidentCommentRequest;
import com.comunidad360.incidents.dto.IncidentCommentResponse;
import com.comunidad360.incidents.dto.IncidentDetailResponse;
import com.comunidad360.incidents.dto.IncidentHistoryResponse;
import com.comunidad360.incidents.dto.IncidentResponse;
import com.comunidad360.incidents.dto.UpdateIncidentRequest;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.property.CommonElement;
import com.comunidad360.property.CommonElementRepository;
import com.comunidad360.property.Unit;
import com.comunidad360.property.UnitRepository;
import com.comunidad360.security.TokenSupport;
import com.comunidad360.storage.StorageService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Map<IncidentStatus, Set<IncidentStatus>> TRANSITIONS = Map.of(
            IncidentStatus.OPEN, Set.of(IncidentStatus.IN_PROGRESS, IncidentStatus.WAITING_PROVIDER,
                    IncidentStatus.CANCELLED),
            IncidentStatus.IN_PROGRESS, Set.of(IncidentStatus.WAITING_PROVIDER, IncidentStatus.RESOLVED,
                    IncidentStatus.CANCELLED),
            IncidentStatus.WAITING_PROVIDER, Set.of(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED,
                    IncidentStatus.CANCELLED),
            IncidentStatus.RESOLVED, Set.of(IncidentStatus.CLOSED, IncidentStatus.IN_PROGRESS),
            IncidentStatus.CLOSED, Set.of(),
            IncidentStatus.CANCELLED, Set.of()
    );

    private final IncidentRepository incidentRepository;
    private final IncidentCommentRepository commentRepository;
    private final IncidentStatusHistoryRepository historyRepository;
    private final IncidentAttachmentRepository attachmentRepository;
    private final IncidentAccessService accessService;
    private final MembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final UnitRepository unitRepository;
    private final BuildingRepository buildingRepository;
    private final CommonElementRepository commonElementRepository;
    private final StorageService storageService;
    private final AuditService auditService;
    private final AppProperties properties;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentCommentRepository commentRepository,
                           IncidentStatusHistoryRepository historyRepository,
                           IncidentAttachmentRepository attachmentRepository,
                           IncidentAccessService accessService,
                           MembershipRepository membershipRepository,
                           AppUserRepository userRepository,
                           UnitRepository unitRepository,
                           BuildingRepository buildingRepository,
                           CommonElementRepository commonElementRepository,
                           StorageService storageService,
                           AuditService auditService,
                           AppProperties properties) {
        this.incidentRepository = incidentRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.attachmentRepository = attachmentRepository;
        this.accessService = accessService;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.unitRepository = unitRepository;
        this.buildingRepository = buildingRepository;
        this.commonElementRepository = commonElementRepository;
        this.storageService = storageService;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> list(UUID communityId, UUID userId, IncidentStatus status,
                                       IncidentPriority priority, boolean assignedToMe) {
        Membership membership = accessService.requireMembership(communityId, userId);
        if (!accessService.canViewAny(membership)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        return incidentRepository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .filter(incident -> status == null || incident.getStatus() == status)
                .filter(incident -> priority == null || incident.getPriority() == priority)
                .filter(incident -> !assignedToMe || userId.equals(incident.getAssignedToUserId()))
                .filter(incident -> accessService.canView(incident, userId, membership))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse detail(UUID communityId, UUID userId, UUID incidentId) {
        Membership membership = accessService.requireMembership(communityId, userId);
        Incident incident = find(communityId, incidentId);
        accessService.requireView(incident, userId, membership);
        boolean staff = accessService.canViewAll(membership);
        List<IncidentCommentResponse> comments = commentRepository
                .findByCommunityIdAndIncidentIdOrderByCreatedAtAsc(communityId, incidentId).stream()
                .filter(comment -> staff || !comment.isInternal())
                .map(comment -> IncidentCommentResponse.from(comment, userName(comment.getAuthorUserId())))
                .toList();
        List<IncidentAttachmentResponse> attachments = attachmentRepository
                .findByCommunityIdAndIncidentIdOrderByCreatedAtAsc(communityId, incidentId).stream()
                .map(this::toAttachmentResponse)
                .toList();
        List<IncidentHistoryResponse> history = historyRepository
                .findByCommunityIdAndIncidentIdOrderByChangedAtAsc(communityId, incidentId).stream()
                .map(entry -> new IncidentHistoryResponse(
                        entry.getId(),
                        entry.getFromStatus(),
                        entry.getToStatus(),
                        userName(entry.getChangedByUserId()),
                        entry.getChangedAt(),
                        entry.getNote()
                ))
                .toList();
        return new IncidentDetailResponse(toResponse(incident), comments, attachments, history);
    }

    @Transactional
    public IncidentResponse create(UUID communityId, UUID userId, CreateIncidentRequest request,
                                   HttpServletRequest httpRequest) {
        accessService.requireCreate(communityId, userId);
        validateLocation(communityId, request.buildingId(), request.unitId(), request.commonElementId());
        Instant now = Instant.now();
        Incident incident = new Incident(
                UUID.randomUUID(),
                communityId,
                request.title().trim(),
                request.description().trim(),
                request.category().trim(),
                request.priority(),
                userId,
                now,
                now
        );
        incident.setBuildingId(request.buildingId());
        incident.setUnitId(request.unitId());
        incident.setCommonElementId(request.commonElementId());
        Incident saved = incidentRepository.saveAndFlush(incident);
        recordHistory(communityId, saved.getId(), null, IncidentStatus.OPEN, userId, now, "Incidencia creada");
        IncidentResponse response = toResponse(saved);
        auditService.record(communityId, userId, "USER", "INCIDENT_CREATED", "INCIDENT",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public IncidentResponse update(UUID communityId, UUID userId, UUID incidentId,
                                   UpdateIncidentRequest request, HttpServletRequest httpRequest) {
        accessService.requireManage(communityId, userId);
        Incident incident = find(communityId, incidentId);
        if (request.version() != null && request.version() != incident.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        IncidentResponse before = toResponse(incident);
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            incident.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            if (request.description().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            incident.setDescription(request.description().trim());
        }
        if (request.category() != null) {
            if (request.category().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            incident.setCategory(request.category().trim());
        }
        if (request.priority() != null) {
            incident.setPriority(request.priority());
        }
        if (request.assignedToUserId() != null) {
            validateAssignee(communityId, request.assignedToUserId());
            incident.setAssignedToUserId(request.assignedToUserId());
        }
        if (request.status() != null && request.status() != incident.getStatus()) {
            validateTransition(incident.getStatus(), request.status());
            IncidentStatus previous = incident.getStatus();
            Instant now = Instant.now();
            incident.setStatus(request.status());
            if (request.status() == IncidentStatus.RESOLVED) {
                incident.setResolvedAt(now);
            } else if (request.status() == IncidentStatus.CLOSED) {
                incident.setClosedAt(now);
            } else if (request.status() == IncidentStatus.IN_PROGRESS) {
                incident.setResolvedAt(null);
            }
            recordHistory(communityId, incidentId, previous, request.status(), userId, now, null);
        }
        incident.setUpdatedAt(Instant.now());
        Incident saved = incidentRepository.saveAndFlush(incident);
        IncidentResponse after = toResponse(saved);
        auditService.record(communityId, userId, "USER", "INCIDENT_UPDATED", "INCIDENT",
                incidentId, before, after, httpRequest);
        return after;
    }

    @Transactional
    public IncidentCommentResponse addComment(UUID communityId, UUID userId, UUID incidentId,
                                              IncidentCommentRequest request, HttpServletRequest httpRequest) {
        Membership membership = accessService.requireMembership(communityId, userId);
        Incident incident = find(communityId, incidentId);
        accessService.requireView(incident, userId, membership);
        if (request.internal() && !accessService.canViewAll(membership)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        Instant now = Instant.now();
        IncidentComment comment = new IncidentComment(
                UUID.randomUUID(),
                communityId,
                incidentId,
                userId,
                request.body().trim(),
                request.internal(),
                now,
                now
        );
        IncidentComment saved = commentRepository.saveAndFlush(comment);
        IncidentCommentResponse response = IncidentCommentResponse.from(saved, userName(userId));
        auditService.record(communityId, userId, "USER", "INCIDENT_COMMENT_CREATED", "INCIDENT",
                incidentId, null, Map.of("commentId", saved.getId(), "internal", request.internal()), httpRequest);
        return response;
    }

    @Transactional
    public IncidentAttachmentResponse uploadAttachment(UUID communityId, UUID userId, UUID incidentId,
                                                       MultipartFile file, HttpServletRequest httpRequest) {
        Membership membership = accessService.requireMembership(communityId, userId);
        Incident incident = find(communityId, incidentId);
        accessService.requireView(incident, userId, membership);
        if (file == null || file.isEmpty()) {
            throw BusinessException.unprocessable("ATTACHMENT_EMPTY", "errors.incident.attachmentInvalid");
        }
        if (file.getSize() > properties.getStorage().getMaxIncidentPhotoBytes()) {
            throw BusinessException.unprocessable("ATTACHMENT_TOO_LARGE", "errors.incident.attachmentSize");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read attachment", exception);
        }
        String extension = extensionFor(content, file.getContentType());
        if (extension == null) {
            throw BusinessException.unprocessable(
                    "ATTACHMENT_TYPE_UNSUPPORTED", "errors.incident.attachmentType");
        }
        String mimeType = switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
        String storageKey = storageService.store(content, extension, mimeType);
        Instant now = Instant.now();
        IncidentAttachment attachment = new IncidentAttachment(
                UUID.randomUUID(),
                communityId,
                incidentId,
                storageKey,
                safeFileName(file.getOriginalFilename(), extension),
                mimeType,
                file.getSize(),
                TokenSupport.sha256Hex(content),
                userId,
                now
        );
        IncidentAttachment saved = attachmentRepository.saveAndFlush(attachment);
        IncidentAttachmentResponse response = toAttachmentResponse(saved);
        auditService.record(communityId, userId, "USER", "INCIDENT_ATTACHMENT_CREATED", "INCIDENT",
                incidentId, null, Map.of("attachmentId", saved.getId()), httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public DownloadFile downloadAttachment(UUID communityId, UUID userId, UUID incidentId, UUID attachmentId) {
        Membership membership = accessService.requireMembership(communityId, userId);
        Incident incident = find(communityId, incidentId);
        accessService.requireView(incident, userId, membership);
        IncidentAttachment attachment = attachmentRepository.findByIdAndCommunityId(attachmentId, communityId)
                .filter(value -> value.getIncidentId().equals(incidentId))
                .orElseThrow(() -> BusinessException.notFound(
                        "ATTACHMENT_NOT_FOUND", "errors.resource.notFound"));
        return new DownloadFile(attachment.getFileName(), attachment.getMimeType(),
                storageService.read(attachment.getStorageKey()));
    }

    private Incident find(UUID communityId, UUID incidentId) {
        return incidentRepository.findByIdAndCommunityId(incidentId, communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "INCIDENT_NOT_FOUND", "errors.incident.notFound"));
    }

    private IncidentResponse toResponse(Incident incident) {
        String unitCode = incident.getUnitId() == null
                ? null
                : unitRepository.findById(incident.getUnitId()).map(Unit::getCode).orElse(null);
        return IncidentResponse.from(
                incident,
                unitCode,
                userName(incident.getReportedByUserId()),
                userName(incident.getAssignedToUserId())
        );
    }

    private IncidentAttachmentResponse toAttachmentResponse(IncidentAttachment attachment) {
        return new IncidentAttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getMimeType(),
                attachment.getSizeBytes(),
                userName(attachment.getUploadedByUserId()),
                attachment.getCreatedAt(),
                "/communities/" + attachment.getCommunityId() + "/incidents/"
                        + attachment.getIncidentId() + "/attachments/" + attachment.getId()
        );
    }

    private void recordHistory(UUID communityId, UUID incidentId, IncidentStatus from, IncidentStatus to,
                               UUID userId, Instant at, String note) {
        historyRepository.save(new IncidentStatusHistory(
                UUID.randomUUID(),
                communityId,
                incidentId,
                from,
                to,
                userId,
                at,
                note
        ));
    }

    private void validateTransition(IncidentStatus from, IncidentStatus to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw BusinessException.conflict(
                    "INVALID_STATUS_TRANSITION", "errors.incident.invalidTransition");
        }
    }

    private void validateAssignee(UUID communityId, UUID userId) {
        membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.unprocessable(
                        "INVALID_ASSIGNEE", "errors.incident.assigneeInvalid"));
    }

    private void validateLocation(UUID communityId, UUID buildingId, UUID unitId, UUID commonElementId) {
        if (buildingId != null) {
            Building building = buildingRepository.findByIdAndCommunityId(buildingId, communityId)
                    .filter(value -> value.getArchivedAt() == null)
                    .orElseThrow(this::invalidLocation);
        }
        if (unitId != null) {
            Unit unit = unitRepository.findByIdAndCommunityId(unitId, communityId)
                    .filter(value -> value.getArchivedAt() == null)
                    .orElseThrow(this::invalidLocation);
        }
        if (commonElementId != null) {
            CommonElement element = commonElementRepository.findByIdAndCommunityId(commonElementId, communityId)
                    .filter(value -> value.getArchivedAt() == null)
                    .orElseThrow(this::invalidLocation);
        }
    }

    private BusinessException invalidLocation() {
        return BusinessException.unprocessable(
                "INVALID_INCIDENT_LOCATION", "errors.incident.invalidLocation");
    }

    private String userName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(user -> user.getFullName() == null || user.getFullName().isBlank()
                        ? user.getEmail()
                        : user.getFullName())
                .orElse(null);
    }

    private String extensionFor(byte[] content, String mimeType) {
        if (content.length >= 3
                && (content[0] & 0xFF) == 0xFF
                && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (content.length >= 8
                && (content[0] & 0xFF) == 0x89
                && content[1] == 'P'
                && content[2] == 'N'
                && content[3] == 'G') {
            return "png";
        }
        if (content.length >= 12
                && content[0] == 'R'
                && content[1] == 'I'
                && content[2] == 'F'
                && content[3] == 'F'
                && content[8] == 'W'
                && content[9] == 'E'
                && content[10] == 'B'
                && content[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private String safeFileName(String original, String extension) {
        String name = original == null || original.isBlank() ? "foto." + extension : original;
        name = name.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!name.toLowerCase().endsWith("." + extension)) {
            name = name + "." + extension;
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    public record DownloadFile(String fileName, String mimeType, byte[] content) {
    }
}

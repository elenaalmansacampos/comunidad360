package com.comunidad360.works;

import com.comunidad360.audit.AuditService;
import com.comunidad360.authorization.AuthorizationScope;
import com.comunidad360.authorization.AuthorizationService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.config.AppProperties;
import com.comunidad360.malware.MalwareScanner;
import com.comunidad360.malware.ScanResult;
import com.comunidad360.malware.ScanStatus;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.storage.StorageService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import com.comunidad360.works.dto.CreateWorkPermitRequest;
import com.comunidad360.works.dto.UpdateWorkPermitRequest;
import com.comunidad360.works.dto.WorkPermitResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkPermitService {

    private final WorkPermitRepository repository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final AppUserRepository userRepository;
    private final StorageService storageService;
    private final MalwareScanner malwareScanner;
    private final AppProperties properties;
    private final AuthorizationService authorizationService;

    public WorkPermitService(WorkPermitRepository repository, TenantAccessService tenantAccessService,
                             PermissionService permissionService, AuditService auditService,
                             AppUserRepository userRepository, StorageService storageService,
                             MalwareScanner malwareScanner, AppProperties properties,
                             AuthorizationService authorizationService) {
        this.repository = repository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.malwareScanner = malwareScanner;
        this.properties = properties;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<WorkPermitResponse> list(UUID communityId, UUID userId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "WORK_PERMIT_VIEW");
        return repository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .map(this::response).toList();
    }

    @Transactional
    public WorkPermitResponse create(UUID communityId, UUID userId, CreateWorkPermitRequest request,
                                     UUID onBehalfOfMembershipId, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        Membership effective = authorizationService.resolveOnBehalf(communityId, membership,
                onBehalfOfMembershipId, AuthorizationScope.WORK_PERMIT);
        permissionService.require(effective, "WORK_PERMIT_CREATE");
        Instant now = Instant.now();
        WorkPermit permit = repository.saveAndFlush(new WorkPermit(UUID.randomUUID(), communityId,
                request.title().trim(), clean(request.description()), clean(request.location()),
                request.startDate(), request.endDate(), WorkPermitStatus.REQUESTED, userId, now, now));
        WorkPermitResponse response = response(permit);
        auditService.record(communityId, userId, "USER", "WORK_PERMIT_CREATED", "WORK_PERMIT",
                permit.getId(), null, authorizationService.auditAfter(userId, effective, response), httpRequest);
        return response;
    }

    @Transactional
    public WorkPermitResponse update(UUID communityId, UUID userId, UUID permitId,
                                     UpdateWorkPermitRequest request, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "WORK_PERMIT_VIEW");
        WorkPermit permit = find(communityId, permitId);
        boolean manage = permissionService.hasPermission(membership, "WORK_PERMIT_MANAGE");
        boolean author = userId.equals(permit.getCreatedByUserId());
        if (!manage && !author) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        if (request.status() != null && request.status() != permit.getStatus()
                && !manage && request.status() != WorkPermitStatus.CANCELLED) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        if (request.reviewNotes() != null && !manage) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        if (request.version() != null && request.version() != permit.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        WorkPermitResponse before = response(permit);
        Instant now = Instant.now();
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            permit.setTitle(request.title().trim());
        }
        if (request.description() != null) permit.setDescription(clean(request.description()));
        if (request.location() != null) permit.setLocation(clean(request.location()));
        if (request.startDate() != null) permit.setStartDate(request.startDate());
        if (request.endDate() != null) permit.setEndDate(request.endDate());
        if (request.status() != null && request.status() != permit.getStatus()) {
            permit.setStatus(request.status());
            if (manage && (request.status() == WorkPermitStatus.APPROVED
                    || request.status() == WorkPermitStatus.REJECTED)) {
                permit.setReviewedByUserId(userId);
                permit.setReviewedAt(now);
            }
        }
        if (request.reviewNotes() != null) permit.setReviewNotes(clean(request.reviewNotes()));
        permit.setUpdatedAt(now);
        WorkPermit saved = repository.saveAndFlush(permit);
        WorkPermitResponse response = response(saved);
        auditService.record(communityId, userId, "USER", "WORK_PERMIT_UPDATED", "WORK_PERMIT",
                permitId, before, response, httpRequest);
        return response;
    }

    @Transactional
    public WorkPermitResponse uploadFile(UUID communityId, UUID userId, UUID permitId,
                                         MultipartFile file, UUID onBehalfOfMembershipId,
                                         HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        Membership effective = authorizationService.resolveOnBehalf(communityId, membership,
                onBehalfOfMembershipId, AuthorizationScope.WORK_PERMIT);
        permissionService.require(membership, "WORK_PERMIT_VIEW");
        permissionService.require(effective, "WORK_PERMIT_CREATE");
        WorkPermit permit = find(communityId, permitId);
        boolean manage = permissionService.hasPermission(membership, "WORK_PERMIT_MANAGE");
        if (!manage && !userId.equals(permit.getCreatedByUserId())) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        if (file == null || file.isEmpty()) {
            throw BusinessException.unprocessable("FILE_EMPTY", "errors.validation");
        }
        if (file.getSize() > properties.getStorage().getMaxDocumentBytes()) {
            throw BusinessException.unprocessable("FILE_TOO_LARGE", "errors.document.scanError");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read work permit file", exception);
        }
        String extension = extensionFor(content);
        if (extension == null) {
            throw BusinessException.unprocessable("FILE_TYPE_UNSUPPORTED", "errors.validation");
        }
        String mimeType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
        ScanResult scan = malwareScanner.scan(content);
        if (scan.status() == ScanStatus.INFECTED) {
            throw BusinessException.unprocessable("FILE_INFECTED", "errors.document.infected");
        }
        if (scan.status() == ScanStatus.ERROR) {
            throw BusinessException.unprocessable("FILE_SCAN_ERROR", "errors.document.scanError");
        }
        String oldKey = permit.getFileKey();
        String storageKey = storageService.store(content, extension, mimeType);
        permit.setFileKey(storageKey);
        permit.setFileContentType(mimeType);
        permit.setFileName(safeFileName(file.getOriginalFilename(), extension));
        permit.setUpdatedAt(Instant.now());
        WorkPermit saved = repository.saveAndFlush(permit);
        if (oldKey != null && !oldKey.equals(storageKey)) {
            try {
                storageService.delete(oldKey);
            } catch (Exception ignored) {
                // el archivo anterior no se pudo borrar; no bloquea la subida
            }
        }
        WorkPermitResponse response = response(saved);
        auditService.record(communityId, userId, "USER", "WORK_PERMIT_FILE_UPLOADED", "WORK_PERMIT",
                permitId, null, authorizationService.auditAfter(userId, effective, response), httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public FileDownload downloadFile(UUID communityId, UUID userId, UUID permitId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "WORK_PERMIT_VIEW");
        WorkPermit permit = find(communityId, permitId);
        if (permit.getFileKey() == null) {
            throw BusinessException.notFound("WORK_PERMIT_FILE_NOT_FOUND", "errors.resource.notFound");
        }
        byte[] content = storageService.read(permit.getFileKey());
        return new FileDownload(permit.getFileName(), permit.getFileContentType(), content);
    }

    @Transactional
    public void delete(UUID communityId, UUID userId, UUID permitId, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "WORK_PERMIT_VIEW");
        WorkPermit permit = find(communityId, permitId);
        boolean manage = permissionService.hasPermission(membership, "WORK_PERMIT_MANAGE");
        if (!manage && !userId.equals(permit.getCreatedByUserId())) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        WorkPermitResponse before = response(permit);
        repository.delete(permit);
        if (permit.getFileKey() != null) {
            try {
                storageService.delete(permit.getFileKey());
            } catch (Exception ignored) {
                // se permite borrar el registro aunque falle el borrado del archivo
            }
        }
        auditService.record(communityId, userId, "USER", "WORK_PERMIT_DELETED", "WORK_PERMIT",
                permitId, before, null, httpRequest);
    }

    private WorkPermit find(UUID communityId, UUID permitId) {
        return repository.findByIdAndCommunityId(permitId, communityId)
                .orElseThrow(() -> BusinessException.notFound("WORK_PERMIT_NOT_FOUND", "errors.resource.notFound"));
    }

    private WorkPermitResponse response(WorkPermit permit) {
        return WorkPermitResponse.from(permit, userName(permit.getCreatedByUserId()),
                userName(permit.getReviewedByUserId()));
    }

    private String userName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(this::name).orElse(null);
    }

    private String name(AppUser user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String extensionFor(byte[] content) {
        if (content.length >= 4
                && content[0] == '%' && content[1] == 'P' && content[2] == 'D' && content[3] == 'F') {
            return "pdf";
        }
        if (content.length >= 3
                && (content[0] & 0xFF) == 0xFF
                && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (content.length >= 8
                && (content[0] & 0xFF) == 0x89
                && content[1] == 'P' && content[2] == 'N' && content[3] == 'G') {
            return "png";
        }
        if (content.length >= 12
                && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private String safeFileName(String original, String extension) {
        String name = original == null || original.isBlank() ? "permiso." + extension : original;
        name = name.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!name.toLowerCase().endsWith("." + extension)) {
            name = name + "." + extension;
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    public record FileDownload(String fileName, String contentType, byte[] content) {
    }
}

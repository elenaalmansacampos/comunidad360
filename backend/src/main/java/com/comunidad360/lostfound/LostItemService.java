package com.comunidad360.lostfound;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.config.AppProperties;
import com.comunidad360.lostfound.dto.CreateLostItemRequest;
import com.comunidad360.lostfound.dto.LostItemResponse;
import com.comunidad360.lostfound.dto.UpdateLostItemRequest;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.malware.MalwareScanner;
import com.comunidad360.malware.ScanResult;
import com.comunidad360.malware.ScanStatus;
import com.comunidad360.storage.StorageService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LostItemService {

    private final LostItemRepository repository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final AppUserRepository userRepository;
    private final StorageService storageService;
    private final MalwareScanner malwareScanner;
    private final AppProperties properties;

    public LostItemService(LostItemRepository repository, TenantAccessService tenantAccessService,
                           PermissionService permissionService, AuditService auditService,
                           AppUserRepository userRepository, StorageService storageService,
                           MalwareScanner malwareScanner, AppProperties properties) {
        this.repository = repository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.malwareScanner = malwareScanner;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<LostItemResponse> list(UUID communityId, UUID userId, LostItemType type, LostItemStatus status) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        return repository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .filter(item -> type == null || item.getType() == type)
                .filter(item -> status == null || item.getStatus() == status)
                .map(item -> LostItemResponse.from(item, userName(item.getCreatedByUserId())))
                .toList();
    }

    @Transactional
    public LostItemResponse create(UUID communityId, UUID userId, CreateLostItemRequest request,
                                   HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_CREATE");
        Instant now = Instant.now();
        LostItem item = repository.saveAndFlush(new LostItem(UUID.randomUUID(), communityId,
                request.title().trim(), clean(request.description()), request.type(), clean(request.location()),
                request.eventDate(), clean(request.contact()), LostItemStatus.OPEN, userId, now, now));
        LostItemResponse response = LostItemResponse.from(item, userName(userId));
        auditService.record(communityId, userId, "USER", "LOST_ITEM_CREATED", "LOST_ITEM",
                item.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public LostItemResponse update(UUID communityId, UUID userId, UUID itemId,
                                   UpdateLostItemRequest request, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        LostItem item = find(communityId, itemId);
        if (!canManage(membership, item, userId)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        if (request.version() != null && request.version() != item.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        LostItemResponse before = LostItemResponse.from(item, userName(item.getCreatedByUserId()));
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            item.setTitle(request.title().trim());
        }
        if (request.description() != null) item.setDescription(clean(request.description()));
        if (request.type() != null) item.setType(request.type());
        if (request.location() != null) item.setLocation(clean(request.location()));
        if (request.eventDate() != null) item.setEventDate(request.eventDate());
        if (request.contact() != null) item.setContact(clean(request.contact()));
        if (request.status() != null) item.setStatus(request.status());
        item.setUpdatedAt(Instant.now());
        LostItem saved = repository.saveAndFlush(item);
        LostItemResponse response = LostItemResponse.from(saved, userName(saved.getCreatedByUserId()));
        auditService.record(communityId, userId, "USER", "LOST_ITEM_UPDATED", "LOST_ITEM",
                itemId, before, response, httpRequest);
        return response;
    }

    @Transactional
    public void delete(UUID communityId, UUID userId, UUID itemId, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        LostItem item = find(communityId, itemId);
        if (!canManage(membership, item, userId)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        LostItemResponse before = LostItemResponse.from(item, userName(item.getCreatedByUserId()));
        repository.delete(item);
        auditService.record(communityId, userId, "USER", "LOST_ITEM_DELETED", "LOST_ITEM",
                itemId, before, null, httpRequest);
    }


    @Transactional
    public LostItemResponse uploadPhoto(UUID communityId, UUID userId, UUID itemId, MultipartFile file,
                                        HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        permissionService.require(membership, "LOST_ITEM_CREATE");
        LostItem item = find(communityId, itemId);
        if (!canManage(membership, item, userId)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        if (file == null || file.isEmpty()) {
            throw BusinessException.unprocessable("PHOTO_EMPTY", "errors.validation");
        }
        if (file.getSize() > properties.getStorage().getMaxIncidentPhotoBytes()) {
            throw BusinessException.unprocessable("PHOTO_TOO_LARGE", "errors.incident.attachmentSize");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read photo", exception);
        }
        String extension = extensionFor(content);
        if (extension == null) {
            throw BusinessException.unprocessable("PHOTO_TYPE_UNSUPPORTED", "errors.incident.attachmentType");
        }
        String mimeType = switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
        ScanResult scan = malwareScanner.scan(content);
        if (scan.status() == ScanStatus.INFECTED) {
            throw BusinessException.unprocessable("PHOTO_INFECTED", "errors.incident.attachmentInfected");
        }
        if (scan.status() == ScanStatus.ERROR) {
            throw BusinessException.unprocessable("PHOTO_SCAN_ERROR", "errors.incident.attachmentScanError");
        }
        String oldKey = item.getPhotoKey();
        String storageKey = storageService.store(content, extension, mimeType);
        item.setPhotoKey(storageKey);
        item.setPhotoContentType(mimeType);
        item.setPhotoFileName(safeFileName(file.getOriginalFilename(), extension));
        item.setUpdatedAt(Instant.now());
        LostItem saved = repository.saveAndFlush(item);
        if (oldKey != null && !oldKey.equals(storageKey)) {
            try {
                storageService.delete(oldKey);
            } catch (Exception ignored) {
                // la foto anterior no se pudo borrar; no bloquea la subida
            }
        }
        LostItemResponse response = LostItemResponse.from(saved, userName(saved.getCreatedByUserId()));
        auditService.record(communityId, userId, "USER", "LOST_ITEM_PHOTO_UPLOADED", "LOST_ITEM",
                itemId, null, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public PhotoFile downloadPhoto(UUID communityId, UUID userId, UUID itemId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        LostItem item = find(communityId, itemId);
        if (item.getPhotoKey() == null) {
            throw BusinessException.notFound("LOST_ITEM_PHOTO_NOT_FOUND", "errors.resource.notFound");
        }
        byte[] content = storageService.read(item.getPhotoKey());
        return new PhotoFile(item.getPhotoFileName(), item.getPhotoContentType(), content);
    }

    private LostItem find(UUID communityId, UUID itemId) {
        return repository.findByIdAndCommunityId(itemId, communityId)
                .orElseThrow(() -> BusinessException.notFound("LOST_ITEM_NOT_FOUND", "errors.resource.notFound"));
    }

    private boolean canManage(Membership membership, LostItem item, UUID userId) {
        return permissionService.hasPermission(membership, "LOST_ITEM_MANAGE")
                || userId.equals(item.getCreatedByUserId());
    }


    private String extensionFor(byte[] content) {
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

    public record PhotoFile(String fileName, String contentType, byte[] content) {
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
}

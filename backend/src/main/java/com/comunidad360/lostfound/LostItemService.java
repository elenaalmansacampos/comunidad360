package com.comunidad360.lostfound;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.config.AppProperties;
import com.comunidad360.lostfound.dto.CreateLostItemNoteRequest;
import com.comunidad360.lostfound.dto.CreateLostItemRequest;
import com.comunidad360.lostfound.dto.LostItemAttachmentResponse;
import com.comunidad360.lostfound.dto.LostItemNoteResponse;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LostItemService {

    private final LostItemRepository repository;
    private final LostItemNoteRepository noteRepository;
    private final LostItemAttachmentRepository attachmentRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final AppUserRepository userRepository;
    private final StorageService storageService;
    private final MalwareScanner malwareScanner;
    private final AppProperties properties;

    public LostItemService(LostItemRepository repository, LostItemNoteRepository noteRepository,
                           LostItemAttachmentRepository attachmentRepository,
                           TenantAccessService tenantAccessService, PermissionService permissionService,
                           AuditService auditService, AppUserRepository userRepository,
                           StorageService storageService, MalwareScanner malwareScanner,
                           AppProperties properties) {
        this.repository = repository;
        this.noteRepository = noteRepository;
        this.attachmentRepository = attachmentRepository;
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
        List<LostItem> items = repository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .filter(item -> type == null || item.getType() == type)
                .filter(item -> status == null || item.getStatus() == status)
                .toList();
        Map<UUID, List<LostItemNote>> notes = noteRepository.findByCommunityIdOrderByCreatedAtAsc(communityId)
                .stream().collect(Collectors.groupingBy(LostItemNote::getItemId));
        Map<UUID, List<LostItemAttachment>> attachments =
                attachmentRepository.findByCommunityIdOrderByCreatedAtAsc(communityId)
                        .stream().collect(Collectors.groupingBy(LostItemAttachment::getItemId));
        Set<UUID> userIds = new HashSet<>();
        items.forEach(item -> addIfPresent(userIds, item.getCreatedByUserId()));
        notes.values().forEach(list -> list.forEach(note -> addIfPresent(userIds, note.getAuthorUserId())));
        attachments.values().forEach(list ->
                list.forEach(attachment -> addIfPresent(userIds, attachment.getUploadedByUserId())));
        Map<UUID, String> names = userNames(userIds);
        return items.stream().map(item -> LostItemResponse.from(item, names.get(item.getCreatedByUserId()),
                toNoteResponses(notes.getOrDefault(item.getId(), List.of()), names),
                toAttachmentResponses(attachments.getOrDefault(item.getId(), List.of()), names))).toList();
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
        LostItemResponse response = toResponse(item);
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
        LostItemResponse before = toResponse(item);
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
        LostItemResponse response = toResponse(saved);
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
        List<LostItemAttachment> attachments =
                attachmentRepository.findByItemIdAndCommunityIdOrderByCreatedAtAsc(itemId, communityId);
        LostItemResponse before = toResponse(item);
        String photoKey = item.getPhotoKey();
        repository.delete(item);
        cleanupStorage(photoKey);
        attachments.forEach(attachment -> cleanupStorage(attachment.getStorageKey()));
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
        byte[] content = read(file);
        String extension = extensionFor(content);
        if (extension == null) {
            throw BusinessException.unprocessable("PHOTO_TYPE_UNSUPPORTED", "errors.incident.attachmentType");
        }
        String mimeType = mimeFor(extension);
        scan(content);
        String oldKey = item.getPhotoKey();
        String storageKey = storageService.store(content, extension, mimeType);
        item.setPhotoKey(storageKey);
        item.setPhotoContentType(mimeType);
        item.setPhotoFileName(safeFileName(file.getOriginalFilename(), extension));
        item.setUpdatedAt(Instant.now());
        LostItem saved = repository.saveAndFlush(item);
        if (oldKey != null && !oldKey.equals(storageKey)) {
            cleanupStorage(oldKey);
        }
        LostItemResponse response = toResponse(saved);
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

    @Transactional
    public LostItemNoteResponse addNote(UUID communityId, UUID userId, UUID itemId,
                                        CreateLostItemNoteRequest request, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        permissionService.require(membership, "LOST_ITEM_CREATE");
        LostItem item = find(communityId, itemId);
        LostItemNote note = noteRepository.saveAndFlush(new LostItemNote(UUID.randomUUID(), communityId,
                itemId, userId, request.body().trim(), Instant.now()));
        LostItemNoteResponse response = LostItemNoteResponse.from(note, userName(userId));
        auditService.record(communityId, userId, "USER", "LOST_ITEM_NOTE_ADDED", "LOST_ITEM",
                item.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public List<LostItemNoteResponse> listNotes(UUID communityId, UUID userId, UUID itemId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        find(communityId, itemId);
        List<LostItemNote> notes =
                noteRepository.findByItemIdAndCommunityIdOrderByCreatedAtAsc(itemId, communityId);
        return toNoteResponses(notes, userNames(idsOfNotes(notes)));
    }

    @Transactional
    public void deleteNote(UUID communityId, UUID userId, UUID itemId, UUID noteId,
                           HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        find(communityId, itemId);
        LostItemNote note = noteRepository.findByIdAndItemIdAndCommunityId(noteId, itemId, communityId)
                .orElseThrow(() -> BusinessException.notFound("LOST_ITEM_NOTE_NOT_FOUND", "errors.resource.notFound"));
        if (!userId.equals(note.getAuthorUserId())
                && !permissionService.hasPermission(membership, "LOST_ITEM_MANAGE")) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        LostItemNoteResponse before = LostItemNoteResponse.from(note, userName(note.getAuthorUserId()));
        noteRepository.delete(note);
        auditService.record(communityId, userId, "USER", "LOST_ITEM_NOTE_DELETED", "LOST_ITEM",
                itemId, before, null, httpRequest);
    }

    @Transactional
    public LostItemAttachmentResponse addAttachment(UUID communityId, UUID userId, UUID itemId,
                                                    MultipartFile file, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        permissionService.require(membership, "LOST_ITEM_CREATE");
        LostItem item = find(communityId, itemId);
        if (file == null || file.isEmpty()) {
            throw BusinessException.unprocessable("ATTACHMENT_EMPTY", "errors.validation");
        }
        if (file.getSize() > properties.getStorage().getMaxDocumentBytes()) {
            throw BusinessException.unprocessable("ATTACHMENT_TOO_LARGE", "errors.incident.attachmentSize");
        }
        byte[] content = read(file);
        String extension = attachmentExtensionFor(content);
        if (extension == null) {
            throw BusinessException.unprocessable("ATTACHMENT_TYPE_UNSUPPORTED", "errors.incident.attachmentType");
        }
        String mimeType = mimeFor(extension);
        scan(content);
        String storageKey = storageService.store(content, extension, mimeType);
        LostItemAttachment attachment = attachmentRepository.saveAndFlush(new LostItemAttachment(
                UUID.randomUUID(), communityId, itemId, storageKey, mimeType,
                safeFileName(file.getOriginalFilename(), extension), content.length, userId, Instant.now()));
        LostItemAttachmentResponse response = LostItemAttachmentResponse.from(attachment, userName(userId));
        auditService.record(communityId, userId, "USER", "LOST_ITEM_ATTACHMENT_ADDED", "LOST_ITEM",
                item.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public List<LostItemAttachmentResponse> listAttachments(UUID communityId, UUID userId, UUID itemId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        find(communityId, itemId);
        List<LostItemAttachment> attachments =
                attachmentRepository.findByItemIdAndCommunityIdOrderByCreatedAtAsc(itemId, communityId);
        return toAttachmentResponses(attachments, userNames(idsOfAttachments(attachments)));
    }

    @Transactional(readOnly = true)
    public AttachmentFile downloadAttachment(UUID communityId, UUID userId, UUID itemId, UUID attachmentId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        find(communityId, itemId);
        LostItemAttachment attachment = attachmentRepository
                .findByIdAndItemIdAndCommunityId(attachmentId, itemId, communityId)
                .orElseThrow(() -> BusinessException.notFound("LOST_ITEM_ATTACHMENT_NOT_FOUND",
                        "errors.resource.notFound"));
        byte[] content = storageService.read(attachment.getStorageKey());
        return new AttachmentFile(attachment.getFileName(), attachment.getContentType(), content);
    }

    @Transactional
    public void deleteAttachment(UUID communityId, UUID userId, UUID itemId, UUID attachmentId,
                                 HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "LOST_ITEM_VIEW");
        find(communityId, itemId);
        LostItemAttachment attachment = attachmentRepository
                .findByIdAndItemIdAndCommunityId(attachmentId, itemId, communityId)
                .orElseThrow(() -> BusinessException.notFound("LOST_ITEM_ATTACHMENT_NOT_FOUND",
                        "errors.resource.notFound"));
        if (!userId.equals(attachment.getUploadedByUserId())
                && !permissionService.hasPermission(membership, "LOST_ITEM_MANAGE")) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        LostItemAttachmentResponse before =
                LostItemAttachmentResponse.from(attachment, userName(attachment.getUploadedByUserId()));
        attachmentRepository.delete(attachment);
        cleanupStorage(attachment.getStorageKey());
        auditService.record(communityId, userId, "USER", "LOST_ITEM_ATTACHMENT_DELETED", "LOST_ITEM",
                itemId, before, null, httpRequest);
    }

    private LostItemResponse toResponse(LostItem item) {
        List<LostItemNote> notes =
                noteRepository.findByItemIdAndCommunityIdOrderByCreatedAtAsc(item.getId(), item.getCommunityId());
        List<LostItemAttachment> attachments =
                attachmentRepository.findByItemIdAndCommunityIdOrderByCreatedAtAsc(item.getId(), item.getCommunityId());
        Set<UUID> userIds = new HashSet<>();
        addIfPresent(userIds, item.getCreatedByUserId());
        notes.forEach(note -> addIfPresent(userIds, note.getAuthorUserId()));
        attachments.forEach(attachment -> addIfPresent(userIds, attachment.getUploadedByUserId()));
        Map<UUID, String> names = userNames(userIds);
        return LostItemResponse.from(item, names.get(item.getCreatedByUserId()),
                toNoteResponses(notes, names), toAttachmentResponses(attachments, names));
    }

    private LostItem find(UUID communityId, UUID itemId) {
        return repository.findByIdAndCommunityId(itemId, communityId)
                .orElseThrow(() -> BusinessException.notFound("LOST_ITEM_NOT_FOUND", "errors.resource.notFound"));
    }

    private boolean canManage(Membership membership, LostItem item, UUID userId) {
        return permissionService.hasPermission(membership, "LOST_ITEM_MANAGE")
                || userId.equals(item.getCreatedByUserId());
    }

    private byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read file", exception);
        }
    }

    private void scan(byte[] content) {
        ScanResult scan = malwareScanner.scan(content);
        if (scan.status() == ScanStatus.INFECTED) {
            throw BusinessException.unprocessable("ATTACHMENT_INFECTED", "errors.incident.attachmentInfected");
        }
        if (scan.status() == ScanStatus.ERROR) {
            throw BusinessException.unprocessable("ATTACHMENT_SCAN_ERROR", "errors.incident.attachmentScanError");
        }
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

    private String attachmentExtensionFor(byte[] content) {
        String image = extensionFor(content);
        if (image != null) {
            return image;
        }
        if (content.length >= 5
                && content[0] == '%'
                && content[1] == 'P'
                && content[2] == 'D'
                && content[3] == 'F'
                && content[4] == '-') {
            return "pdf";
        }
        return null;
    }

    private String mimeFor(String extension) {
        return switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    private String safeFileName(String original, String extension) {
        String name = original == null || original.isBlank() ? "archivo." + extension : original;
        name = name.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!name.toLowerCase().endsWith("." + extension)) {
            name = name + "." + extension;
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private void cleanupStorage(String key) {
        if (key == null) {
            return;
        }
        try {
            storageService.delete(key);
        } catch (Exception ignored) {
            // el archivo huérfano no bloquea la operación
        }
    }

    private void addIfPresent(Set<UUID> ids, UUID id) {
        if (id != null) {
            ids.add(id);
        }
    }

    private Map<UUID, String> userNames(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(AppUser::getId, this::name));
    }

    private Set<UUID> idsOfNotes(List<LostItemNote> notes) {
        Set<UUID> ids = new HashSet<>();
        notes.forEach(note -> addIfPresent(ids, note.getAuthorUserId()));
        return ids;
    }

    private Set<UUID> idsOfAttachments(List<LostItemAttachment> attachments) {
        Set<UUID> ids = new HashSet<>();
        attachments.forEach(attachment -> addIfPresent(ids, attachment.getUploadedByUserId()));
        return ids;
    }

    private List<LostItemNoteResponse> toNoteResponses(List<LostItemNote> notes, Map<UUID, String> names) {
        return notes.stream().map(note ->
                LostItemNoteResponse.from(note, names.get(note.getAuthorUserId()))).toList();
    }

    private List<LostItemAttachmentResponse> toAttachmentResponses(List<LostItemAttachment> attachments,
                                                                   Map<UUID, String> names) {
        return attachments.stream().map(attachment ->
                LostItemAttachmentResponse.from(attachment,
                        names.get(attachment.getUploadedByUserId()))).toList();
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

    public record PhotoFile(String fileName, String contentType, byte[] content) {
    }

    public record AttachmentFile(String fileName, String contentType, byte[] content) {
    }
}

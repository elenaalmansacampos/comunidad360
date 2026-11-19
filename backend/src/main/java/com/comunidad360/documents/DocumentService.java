package com.comunidad360.documents;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.config.AppProperties;
import com.comunidad360.documents.dto.DocumentDetailResponse;
import com.comunidad360.documents.dto.DocumentResponse;
import com.comunidad360.documents.dto.DocumentVersionResponse;
import com.comunidad360.documents.dto.UpdateDocumentRequest;
import com.comunidad360.membership.Membership;
import com.comunidad360.security.RequestInfo;
import com.comunidad360.security.TokenSupport;
import com.comunidad360.storage.StorageService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentAccessLogRepository accessLogRepository;
    private final DocumentAccessService accessService;
    private final AppUserRepository userRepository;
    private final StorageService storageService;
    private final AuditService auditService;
    private final AppProperties properties;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentVersionRepository versionRepository,
                           DocumentAccessLogRepository accessLogRepository,
                           DocumentAccessService accessService,
                           AppUserRepository userRepository,
                           StorageService storageService,
                           AuditService auditService,
                           AppProperties properties) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.accessLogRepository = accessLogRepository;
        this.accessService = accessService;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID communityId, UUID userId, DocumentCategory category) {
        Membership membership = accessService.requireMembership(communityId, userId);
        accessService.requireViewPermission(membership);
        return documentRepository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .filter(document -> document.getStatus() != DocumentStatus.ARCHIVED)
                .filter(document -> category == null || document.getCategory() == category)
                .filter(document -> accessService.canView(document, userId, membership))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse detail(UUID communityId, UUID userId, UUID documentId) {
        Membership membership = accessService.requireMembership(communityId, userId);
        Document document = find(communityId, documentId);
        accessService.requireView(document, userId, membership);
        List<DocumentVersionResponse> versions = versionRepository
                .findByCommunityIdAndDocumentIdOrderByVersionNumberDesc(communityId, documentId).stream()
                .map(this::toVersionResponse)
                .toList();
        return new DocumentDetailResponse(toResponse(document), versions);
    }

    @Transactional
    public DocumentResponse create(UUID communityId, UUID userId, String title, String description,
                                   DocumentCategory category, DocumentVisibility visibility,
                                   MultipartFile file, HttpServletRequest httpRequest) {
        Membership membership = accessService.requireMembership(communityId, userId);
        accessService.requireUpload(membership);
        if (title == null || title.isBlank() || category == null || visibility == null) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
        }
        StoredFile stored = storeFile(file);
        Instant now = Instant.now();
        Document document = new Document(
                UUID.randomUUID(),
                communityId,
                category,
                title.trim(),
                description == null || description.isBlank() ? null : description.trim(),
                visibility,
                userId,
                userId,
                now,
                now
        );
        Document saved = documentRepository.saveAndFlush(document);
        DocumentVersion version = versionRepository.saveAndFlush(new DocumentVersion(
                UUID.randomUUID(),
                communityId,
                saved.getId(),
                1,
                stored.storageKey(),
                stored.fileName(),
                stored.mimeType(),
                stored.sizeBytes(),
                stored.sha256(),
                userId,
                now,
                DocumentScanStatus.CLEAN
        ));
        saved.setCurrentVersionId(version.getId());
        saved.setUpdatedAt(now);
        Document updated = documentRepository.saveAndFlush(saved);
        logAccess(communityId, updated.getId(), version.getId(), userId, DocumentAccessAction.UPLOAD, httpRequest);
        auditService.record(communityId, userId, "USER", "DOCUMENT_CREATED", "DOCUMENT",
                updated.getId(), null, toResponse(updated), httpRequest);
        return toResponse(updated);
    }

    @Transactional
    public DocumentResponse addVersion(UUID communityId, UUID userId, UUID documentId,
                                       MultipartFile file, HttpServletRequest httpRequest) {
        Membership membership = accessService.requireMembership(communityId, userId);
        Document document = find(communityId, documentId);
        accessService.requireView(document, userId, membership);
        accessService.requireUpload(membership);
        StoredFile stored = storeFile(file);
        int nextVersion = versionRepository
                .findFirstByCommunityIdAndDocumentIdOrderByVersionNumberDesc(communityId, documentId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
        Instant now = Instant.now();
        DocumentVersion version = versionRepository.saveAndFlush(new DocumentVersion(
                UUID.randomUUID(),
                communityId,
                documentId,
                nextVersion,
                stored.storageKey(),
                stored.fileName(),
                stored.mimeType(),
                stored.sizeBytes(),
                stored.sha256(),
                userId,
                now,
                DocumentScanStatus.CLEAN
        ));
        document.setCurrentVersionId(version.getId());
        document.setUpdatedAt(now);
        Document saved = documentRepository.saveAndFlush(document);
        logAccess(communityId, documentId, version.getId(), userId, DocumentAccessAction.UPLOAD, httpRequest);
        auditService.record(communityId, userId, "USER", "DOCUMENT_VERSION_CREATED", "DOCUMENT",
                documentId, null, toResponse(saved), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public DocumentResponse update(UUID communityId, UUID userId, UUID documentId,
                                   UpdateDocumentRequest request, HttpServletRequest httpRequest) {
        Membership membership = accessService.requireMembership(communityId, userId);
        Document document = find(communityId, documentId);
        accessService.requireManage(membership);
        if (request.version() != null && request.version() != document.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        DocumentResponse before = toResponse(document);
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            document.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            document.setDescription(request.description().isBlank() ? null : request.description().trim());
        }
        if (request.category() != null) {
            document.setCategory(request.category());
        }
        if (request.visibility() != null) {
            document.setVisibility(request.visibility());
        }
        if (request.status() != null) {
            document.setStatus(request.status());
        }
        document.setUpdatedAt(Instant.now());
        Document saved = documentRepository.saveAndFlush(document);
        logAccess(communityId, documentId, null, userId, DocumentAccessAction.UPDATE, httpRequest);
        DocumentResponse after = toResponse(saved);
        auditService.record(communityId, userId, "USER", "DOCUMENT_UPDATED", "DOCUMENT",
                documentId, before, after, httpRequest);
        return after;
    }

    @Transactional
    public void archive(UUID communityId, UUID userId, UUID documentId, HttpServletRequest httpRequest) {
        Membership membership = accessService.requireMembership(communityId, userId);
        Document document = find(communityId, documentId);
        accessService.requireManage(membership);
        DocumentResponse before = toResponse(document);
        document.setStatus(DocumentStatus.ARCHIVED);
        document.setUpdatedAt(Instant.now());
        Document saved = documentRepository.saveAndFlush(document);
        logAccess(communityId, documentId, null, userId, DocumentAccessAction.DELETE, httpRequest);
        auditService.record(communityId, userId, "USER", "DOCUMENT_ARCHIVED", "DOCUMENT",
                documentId, before, toResponse(saved), httpRequest);
    }

    @Transactional
    public DownloadFile download(UUID communityId, UUID userId, UUID documentId, UUID versionId,
                                 HttpServletRequest httpRequest) {
        Membership membership = accessService.requireMembership(communityId, userId);
        Document document = find(communityId, documentId);
        accessService.requireView(document, userId, membership);
        DocumentVersion version = versionId == null
                ? currentVersion(document)
                : versionRepository.findByIdAndCommunityId(versionId, communityId)
                        .filter(value -> value.getDocumentId().equals(documentId))
                        .orElseThrow(() -> BusinessException.notFound(
                                "DOCUMENT_VERSION_NOT_FOUND", "errors.resource.notFound"));
        logAccess(communityId, documentId, version.getId(), userId, DocumentAccessAction.DOWNLOAD, httpRequest);
        return new DownloadFile(version.getFileName(), version.getMimeType(),
                storageService.read(version.getStorageKey()));
    }

    private Document find(UUID communityId, UUID documentId) {
        return documentRepository.findByIdAndCommunityId(documentId, communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "DOCUMENT_NOT_FOUND", "errors.resource.notFound"));
    }

    private DocumentResponse toResponse(Document document) {
        DocumentVersion current = currentVersionOrNull(document);
        return DocumentResponse.from(
                document,
                current == null ? null : current.getVersionNumber(),
                current == null ? null : current.getFileName(),
                current == null ? null : current.getMimeType(),
                current == null ? null : current.getSizeBytes(),
                current == null ? null : downloadUrl(document, current, false)
        );
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion version) {
        Document document = documentRepository.findById(version.getDocumentId()).orElse(null);
        String url = document == null ? null : downloadUrl(document, version, true);
        return DocumentVersionResponse.from(version, userName(version.getUploadedByUserId()), url);
    }

    private DocumentVersion currentVersion(Document document) {
        DocumentVersion version = currentVersionOrNull(document);
        if (version == null) {
            throw BusinessException.notFound("DOCUMENT_VERSION_NOT_FOUND", "errors.resource.notFound");
        }
        return version;
    }

    private DocumentVersion currentVersionOrNull(Document document) {
        if (document.getCurrentVersionId() == null) {
            return null;
        }
        return versionRepository.findById(document.getCurrentVersionId())
                .filter(version -> version.getCommunityId().equals(document.getCommunityId()))
                .orElse(null);
    }

    private String downloadUrl(Document document, DocumentVersion version, boolean includeVersion) {
        if (storageService.supportsPresignedUrls()) {
            return storageService.presignedGetUrl(
                    version.getStorageKey(),
                    Duration.ofSeconds(properties.getStorage().getS3().getSignedUrlTtlSeconds())
            );
        }
        String base = "/communities/" + document.getCommunityId() + "/documents/" + document.getId()
                + "/download";
        return includeVersion ? base + "?versionId=" + version.getId() : base;
    }

    private StoredFile storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.unprocessable("DOCUMENT_EMPTY", "errors.validation");
        }
        if (file.getSize() > properties.getStorage().getMaxDocumentBytes()) {
            throw BusinessException.unprocessable("DOCUMENT_TOO_LARGE", "errors.validation");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read document", exception);
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String extension = extensionFor(contentType, content);
        if (extension == null) {
            throw BusinessException.unprocessable("DOCUMENT_TYPE_UNSUPPORTED", "errors.validation");
        }
        String storageKey = storageService.store(content, extension, contentType);
        return new StoredFile(
                storageKey,
                safeFileName(file.getOriginalFilename(), extension),
                contentType,
                file.getSize(),
                TokenSupport.sha256Hex(content)
        );
    }

    private String extensionFor(String contentType, byte[] content) {
        if ("application/pdf".equals(contentType) && startsWith(content, "%PDF".getBytes())) {
            return "pdf";
        }
        if ("image/jpeg".equals(contentType) && content.length >= 3
                && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if ("image/png".equals(contentType) && content.length >= 4
                && (content[0] & 0xFF) == 0x89 && content[1] == 'P'
                && content[2] == 'N' && content[3] == 'G') {
            return "png";
        }
        if ("image/webp".equals(contentType) && content.length >= 12
                && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            return "webp";
        }
        if (("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)
                || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType))
                && content.length >= 4 && content[0] == 'P' && content[1] == 'K') {
            return "docx".equals(contentType) ? "docx" : "xlsx";
        }
        if ("text/plain".equals(contentType)) {
            return "txt";
        }
        return null;
    }

    private boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (content[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private String safeFileName(String original, String extension) {
        String name = original == null || original.isBlank() ? "documento." + extension : original;
        name = name.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!name.toLowerCase().endsWith("." + extension)) {
            name = name + "." + extension;
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private void logAccess(UUID communityId, UUID documentId, UUID versionId, UUID userId,
                           DocumentAccessAction action, HttpServletRequest request) {
        accessLogRepository.save(new DocumentAccessLog(
                UUID.randomUUID(),
                communityId,
                documentId,
                versionId,
                userId,
                action,
                request == null ? null : RequestInfo.ipHash(request),
                request == null ? null : RequestInfo.userAgent(request),
                Instant.now()
        ));
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

    private record StoredFile(String storageKey, String fileName, String mimeType, Long sizeBytes,
                              String sha256) {
    }

    public record DownloadFile(String fileName, String mimeType, byte[] content) {
    }
}

package com.comunidad360.lostfound;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.lostfound.dto.CreateLostItemRequest;
import com.comunidad360.lostfound.dto.LostItemResponse;
import com.comunidad360.lostfound.dto.UpdateLostItemRequest;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public LostItemService(LostItemRepository repository, TenantAccessService tenantAccessService,
                           PermissionService permissionService, AuditService auditService,
                           AppUserRepository userRepository) {
        this.repository = repository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.userRepository = userRepository;
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
        permissionService.require(membership, "LOST_ITEM_VIEW");
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

    private LostItem find(UUID communityId, UUID itemId) {
        return repository.findByIdAndCommunityId(itemId, communityId)
                .orElseThrow(() -> BusinessException.notFound("LOST_ITEM_NOT_FOUND", "errors.resource.notFound"));
    }

    private boolean canManage(Membership membership, LostItem item, UUID userId) {
        return permissionService.hasPermission(membership, "LOST_ITEM_MANAGE")
                || userId.equals(item.getCreatedByUserId());
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

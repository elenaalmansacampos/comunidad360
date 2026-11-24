package com.comunidad360.board;

import com.comunidad360.audit.AuditService;
import com.comunidad360.authorization.AuthorizationScope;
import com.comunidad360.authorization.AuthorizationService;
import com.comunidad360.board.dto.BoardNoteCommentResponse;
import com.comunidad360.board.dto.BoardNoteResponse;
import com.comunidad360.board.dto.CreateBoardNoteCommentRequest;
import com.comunidad360.board.dto.CreateBoardNoteRequest;
import com.comunidad360.board.dto.UpdateBoardNoteRequest;
import com.comunidad360.common.exception.BusinessException;
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
public class BoardNoteService {

    private final BoardNoteRepository repository;
    private final BoardNoteCommentRepository commentRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final AppUserRepository userRepository;
    private final AuthorizationService authorizationService;

    public BoardNoteService(BoardNoteRepository repository, BoardNoteCommentRepository commentRepository,
                            TenantAccessService tenantAccessService, PermissionService permissionService,
                            AuditService auditService, AppUserRepository userRepository,
                            AuthorizationService authorizationService) {
        this.repository = repository;
        this.commentRepository = commentRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<BoardNoteResponse> list(UUID communityId, UUID userId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "BOARD_VIEW");
        return repository.findByCommunityIdOrderByPinnedDescCreatedAtDesc(communityId).stream()
                .map(note -> BoardNoteResponse.from(note, userName(note.getCreatedByUserId()),
                        commentRepository.countByCommunityIdAndNoteId(communityId, note.getId())))
                .toList();
    }

    @Transactional
    public BoardNoteResponse create(UUID communityId, UUID userId, CreateBoardNoteRequest request,
                                    UUID onBehalfOfMembershipId, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        Membership effective = authorizationService.resolveOnBehalf(communityId, membership,
                onBehalfOfMembershipId, AuthorizationScope.BOARD);
        permissionService.require(effective, "BOARD_CREATE");
        Instant now = Instant.now();
        BoardNote note = repository.saveAndFlush(new BoardNote(UUID.randomUUID(), communityId,
                request.title().trim(), request.body().trim(), request.color(), false, userId, now, now));
        BoardNoteResponse response = BoardNoteResponse.from(note, userName(userId), 0);
        auditService.record(communityId, userId, "USER", "BOARD_NOTE_CREATED", "BOARD_NOTE",
                note.getId(), null, authorizationService.auditAfter(userId, effective, response), httpRequest);
        return response;
    }

    @Transactional
    public BoardNoteResponse update(UUID communityId, UUID userId, UUID noteId,
                                    UpdateBoardNoteRequest request, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "BOARD_VIEW");
        BoardNote note = find(communityId, noteId);
        if (!canManage(membership, note, userId)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        if (request.version() != null && request.version() != note.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        BoardNoteResponse before = BoardNoteResponse.from(note, userName(note.getCreatedByUserId()),
                commentRepository.countByCommunityIdAndNoteId(communityId, noteId));
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            note.setTitle(request.title().trim());
        }
        if (request.body() != null) {
            if (request.body().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            note.setBody(request.body().trim());
        }
        if (request.color() != null) note.setColor(request.color());
        if (request.pinned() != null) note.setPinned(request.pinned());
        note.setUpdatedAt(Instant.now());
        BoardNote saved = repository.saveAndFlush(note);
        BoardNoteResponse response = BoardNoteResponse.from(saved, userName(saved.getCreatedByUserId()),
                commentRepository.countByCommunityIdAndNoteId(communityId, noteId));
        auditService.record(communityId, userId, "USER", "BOARD_NOTE_UPDATED", "BOARD_NOTE",
                noteId, before, response, httpRequest);
        return response;
    }

    @Transactional
    public void delete(UUID communityId, UUID userId, UUID noteId, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "BOARD_VIEW");
        BoardNote note = find(communityId, noteId);
        if (!canManage(membership, note, userId)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        BoardNoteResponse before = BoardNoteResponse.from(note, userName(note.getCreatedByUserId()),
                commentRepository.countByCommunityIdAndNoteId(communityId, noteId));
        repository.delete(note);
        auditService.record(communityId, userId, "USER", "BOARD_NOTE_DELETED", "BOARD_NOTE",
                noteId, before, null, httpRequest);
    }

    @Transactional(readOnly = true)
    public List<BoardNoteCommentResponse> comments(UUID communityId, UUID userId, UUID noteId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "BOARD_VIEW");
        find(communityId, noteId);
        return commentRepository.findByCommunityIdAndNoteIdOrderByCreatedAtAsc(communityId, noteId).stream()
                .map(comment -> BoardNoteCommentResponse.from(comment, userName(comment.getAuthorUserId())))
                .toList();
    }

    @Transactional
    public BoardNoteCommentResponse addComment(UUID communityId, UUID userId, UUID noteId,
                                               CreateBoardNoteCommentRequest request,
                                               UUID onBehalfOfMembershipId,
                                               HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        Membership effective = authorizationService.resolveOnBehalf(communityId, membership,
                onBehalfOfMembershipId, AuthorizationScope.BOARD);
        permissionService.require(membership, "BOARD_VIEW");
        permissionService.require(effective, "BOARD_CREATE");
        find(communityId, noteId);
        BoardNoteComment comment = commentRepository.saveAndFlush(new BoardNoteComment(UUID.randomUUID(),
                communityId, noteId, userId, request.body().trim(), Instant.now()));
        BoardNoteCommentResponse response = BoardNoteCommentResponse.from(comment, userName(userId));
        auditService.record(communityId, userId, "USER", "BOARD_NOTE_COMMENT_CREATED", "BOARD_NOTE",
                noteId, null, authorizationService.auditAfter(userId, effective, response), httpRequest);
        return response;
    }

    private BoardNote find(UUID communityId, UUID noteId) {
        return repository.findByIdAndCommunityId(noteId, communityId)
                .orElseThrow(() -> BusinessException.notFound("BOARD_NOTE_NOT_FOUND", "errors.resource.notFound"));
    }

    private boolean canManage(Membership membership, BoardNote note, UUID userId) {
        return permissionService.hasPermission(membership, "BOARD_MANAGE")
                || userId.equals(note.getCreatedByUserId());
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
}

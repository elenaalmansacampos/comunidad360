package com.comunidad360.board;

import com.comunidad360.board.dto.BoardNoteCommentResponse;
import com.comunidad360.board.dto.BoardNoteResponse;
import com.comunidad360.board.dto.CreateBoardNoteCommentRequest;
import com.comunidad360.board.dto.CreateBoardNoteRequest;
import com.comunidad360.board.dto.UpdateBoardNoteRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/board-notes")
public class BoardNoteController {

    private final BoardNoteService service;

    public BoardNoteController(BoardNoteService service) {
        this.service = service;
    }

    @GetMapping
    public List<BoardNoteResponse> list(@PathVariable UUID communityId,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(communityId, user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoardNoteResponse create(@PathVariable UUID communityId,
                                    @AuthenticationPrincipal AuthenticatedUser user,
                                    @Valid @RequestBody CreateBoardNoteRequest request,
                                    @RequestParam(required = false) UUID onBehalfOfMembershipId,
                                    HttpServletRequest httpRequest) {
        return service.create(communityId, user.id(), request, onBehalfOfMembershipId, httpRequest);
    }

    @PatchMapping("/{noteId}")
    public BoardNoteResponse update(@PathVariable UUID communityId, @PathVariable UUID noteId,
                                    @AuthenticationPrincipal AuthenticatedUser user,
                                    @Valid @RequestBody UpdateBoardNoteRequest request,
                                    HttpServletRequest httpRequest) {
        return service.update(communityId, user.id(), noteId, request, httpRequest);
    }

    @GetMapping("/{noteId}/comments")
    public List<BoardNoteCommentResponse> comments(@PathVariable UUID communityId,
                                                   @PathVariable UUID noteId,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return service.comments(communityId, user.id(), noteId);
    }

    @PostMapping("/{noteId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public BoardNoteCommentResponse addComment(@PathVariable UUID communityId,
                                               @PathVariable UUID noteId,
                                               @AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CreateBoardNoteCommentRequest request,
                                               @RequestParam(required = false) UUID onBehalfOfMembershipId,
                                               HttpServletRequest httpRequest) {
        return service.addComment(communityId, user.id(), noteId, request, onBehalfOfMembershipId, httpRequest);
    }

    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID communityId, @PathVariable UUID noteId,
                       @AuthenticationPrincipal AuthenticatedUser user,
                       HttpServletRequest httpRequest) {
        service.delete(communityId, user.id(), noteId, httpRequest);
    }
}

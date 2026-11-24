package com.comunidad360.lostfound;

import com.comunidad360.lostfound.dto.CreateLostItemNoteRequest;
import com.comunidad360.lostfound.dto.CreateLostItemRequest;
import com.comunidad360.lostfound.dto.LostItemAttachmentResponse;
import com.comunidad360.lostfound.dto.LostItemNoteResponse;
import com.comunidad360.lostfound.dto.LostItemResponse;
import com.comunidad360.lostfound.dto.UpdateLostItemRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/lost-items")
public class LostItemController {

    private final LostItemService service;

    public LostItemController(LostItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<LostItemResponse> list(@PathVariable UUID communityId,
                                       @RequestParam(required = false) LostItemType type,
                                       @RequestParam(required = false) LostItemStatus status,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(communityId, user.id(), type, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LostItemResponse create(@PathVariable UUID communityId,
                                   @AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody CreateLostItemRequest request,
                                   @RequestParam(required = false) UUID onBehalfOfMembershipId,
                                   HttpServletRequest httpRequest) {
        return service.create(communityId, user.id(), request, onBehalfOfMembershipId, httpRequest);
    }

    @PatchMapping("/{itemId}")
    public LostItemResponse update(@PathVariable UUID communityId, @PathVariable UUID itemId,
                                   @AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody UpdateLostItemRequest request,
                                   HttpServletRequest httpRequest) {
        return service.update(communityId, user.id(), itemId, request, httpRequest);
    }

    @PostMapping(value = "/{itemId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LostItemResponse uploadPhoto(@PathVariable UUID communityId, @PathVariable UUID itemId,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        @RequestPart("file") MultipartFile file,
                                        @RequestParam(required = false) UUID onBehalfOfMembershipId,
                                        HttpServletRequest httpRequest) {
        return service.uploadPhoto(communityId, user.id(), itemId, file, onBehalfOfMembershipId, httpRequest);
    }

    @GetMapping("/{itemId}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable UUID communityId, @PathVariable UUID itemId,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        LostItemService.PhotoFile photo = service.downloadPhoto(communityId, user.id(), itemId);
        String fileName = photo.fileName() == null ? "foto" : photo.fileName();
        String contentType = photo.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : photo.contentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(photo.content().length)
                .body(photo.content());
    }

    @GetMapping("/{itemId}/notes")
    public List<LostItemNoteResponse> notes(@PathVariable UUID communityId, @PathVariable UUID itemId,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.listNotes(communityId, user.id(), itemId);
    }

    @PostMapping("/{itemId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public LostItemNoteResponse addNote(@PathVariable UUID communityId, @PathVariable UUID itemId,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody CreateLostItemNoteRequest request,
                                        @RequestParam(required = false) UUID onBehalfOfMembershipId,
                                        HttpServletRequest httpRequest) {
        return service.addNote(communityId, user.id(), itemId, request, onBehalfOfMembershipId, httpRequest);
    }

    @DeleteMapping("/{itemId}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable UUID communityId, @PathVariable UUID itemId,
                           @PathVariable UUID noteId,
                           @AuthenticationPrincipal AuthenticatedUser user,
                           HttpServletRequest httpRequest) {
        service.deleteNote(communityId, user.id(), itemId, noteId, httpRequest);
    }

    @GetMapping("/{itemId}/attachments")
    public List<LostItemAttachmentResponse> attachments(@PathVariable UUID communityId,
                                                        @PathVariable UUID itemId,
                                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return service.listAttachments(communityId, user.id(), itemId);
    }

    @PostMapping(value = "/{itemId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LostItemAttachmentResponse addAttachment(@PathVariable UUID communityId,
                                                    @PathVariable UUID itemId,
                                                    @AuthenticationPrincipal AuthenticatedUser user,
                                                    @RequestPart("file") MultipartFile file,
                                                    @RequestParam(required = false) UUID onBehalfOfMembershipId,
                                                    HttpServletRequest httpRequest) {
        return service.addAttachment(communityId, user.id(), itemId, file, onBehalfOfMembershipId, httpRequest);
    }

    @GetMapping("/{itemId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> attachment(@PathVariable UUID communityId, @PathVariable UUID itemId,
                                             @PathVariable UUID attachmentId,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        LostItemService.AttachmentFile file =
                service.downloadAttachment(communityId, user.id(), itemId, attachmentId);
        String fileName = file.fileName() == null ? "archivo" : file.fileName();
        String contentType = file.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.contentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(file.content().length)
                .body(file.content());
    }

    @DeleteMapping("/{itemId}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@PathVariable UUID communityId, @PathVariable UUID itemId,
                                 @PathVariable UUID attachmentId,
                                 @AuthenticationPrincipal AuthenticatedUser user,
                                 HttpServletRequest httpRequest) {
        service.deleteAttachment(communityId, user.id(), itemId, attachmentId, httpRequest);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID communityId, @PathVariable UUID itemId,
                       @AuthenticationPrincipal AuthenticatedUser user,
                       HttpServletRequest httpRequest) {
        service.delete(communityId, user.id(), itemId, httpRequest);
    }
}

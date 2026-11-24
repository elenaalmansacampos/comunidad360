package com.comunidad360.incidents;

import com.comunidad360.incidents.dto.CreateIncidentRequest;
import com.comunidad360.incidents.dto.IncidentAttachmentResponse;
import com.comunidad360.incidents.dto.IncidentCommentRequest;
import com.comunidad360.incidents.dto.IncidentCommentResponse;
import com.comunidad360.incidents.dto.IncidentDetailResponse;
import com.comunidad360.incidents.dto.IncidentResponse;
import com.comunidad360.incidents.dto.UpdateIncidentRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public List<IncidentResponse> list(@PathVariable UUID communityId,
                                       @RequestParam(required = false) IncidentStatus status,
                                       @RequestParam(required = false) IncidentPriority priority,
                                       @RequestParam(required = false, defaultValue = "false") boolean assignedToMe,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return incidentService.list(communityId, user.id(), status, priority, assignedToMe);
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@PathVariable UUID communityId,
                                                   @AuthenticationPrincipal AuthenticatedUser user,
                                                   @Valid @RequestBody CreateIncidentRequest request,
                                                   @RequestParam(required = false) UUID onBehalfOfMembershipId,
                                                   HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.create(communityId, user.id(), request, onBehalfOfMembershipId, httpRequest));
    }

    @GetMapping("/{incidentId}")
    public IncidentDetailResponse detail(@PathVariable UUID communityId,
                                         @PathVariable UUID incidentId,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return incidentService.detail(communityId, user.id(), incidentId);
    }

    @PatchMapping("/{incidentId}")
    public IncidentResponse update(@PathVariable UUID communityId,
                                   @PathVariable UUID incidentId,
                                   @AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody UpdateIncidentRequest request,
                                   HttpServletRequest httpRequest) {
        return incidentService.update(communityId, user.id(), incidentId, request, httpRequest);
    }

    @PostMapping("/{incidentId}/comments")
    public ResponseEntity<IncidentCommentResponse> comment(@PathVariable UUID communityId,
                                                           @PathVariable UUID incidentId,
                                                           @AuthenticationPrincipal AuthenticatedUser user,
                                                           @Valid @RequestBody IncidentCommentRequest request,
                                                           @RequestParam(required = false) UUID onBehalfOfMembershipId,
                                                           HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.addComment(communityId, user.id(), incidentId, request,
                        onBehalfOfMembershipId, httpRequest));
    }

    @PostMapping("/{incidentId}/attachments")
    public ResponseEntity<IncidentAttachmentResponse> uploadAttachment(
            @PathVariable UUID communityId,
            @PathVariable UUID incidentId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) UUID onBehalfOfMembershipId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.uploadAttachment(communityId, user.id(), incidentId, file,
                        onBehalfOfMembershipId, httpRequest));
    }

    @GetMapping("/{incidentId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID communityId,
                                                       @PathVariable UUID incidentId,
                                                       @PathVariable UUID attachmentId,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
        IncidentService.DownloadFile file = incidentService.downloadAttachment(
                communityId, user.id(), incidentId, attachmentId);
        ByteArrayResource resource = new ByteArrayResource(file.content());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .contentLength(file.content().length)
                .body(resource);
    }
}

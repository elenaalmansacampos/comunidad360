package com.comunidad360.works;

import com.comunidad360.security.AuthenticatedUser;
import com.comunidad360.works.dto.CreateWorkPermitRequest;
import com.comunidad360.works.dto.UpdateWorkPermitRequest;
import com.comunidad360.works.dto.WorkPermitResponse;
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
@RequestMapping("/api/v1/communities/{communityId}/work-permits")
public class WorkPermitController {

    private final WorkPermitService service;

    public WorkPermitController(WorkPermitService service) {
        this.service = service;
    }

    @GetMapping
    public List<WorkPermitResponse> list(@PathVariable UUID communityId,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(communityId, user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkPermitResponse create(@PathVariable UUID communityId,
                                     @AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody CreateWorkPermitRequest request,
                                     @RequestParam(required = false) UUID onBehalfOfMembershipId,
                                     HttpServletRequest httpRequest) {
        return service.create(communityId, user.id(), request, onBehalfOfMembershipId, httpRequest);
    }

    @PatchMapping("/{permitId}")
    public WorkPermitResponse update(@PathVariable UUID communityId, @PathVariable UUID permitId,
                                     @AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody UpdateWorkPermitRequest request,
                                     HttpServletRequest httpRequest) {
        return service.update(communityId, user.id(), permitId, request, httpRequest);
    }

    @PostMapping(value = "/{permitId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public WorkPermitResponse uploadFile(@PathVariable UUID communityId, @PathVariable UUID permitId,
                                         @AuthenticationPrincipal AuthenticatedUser user,
                                         @RequestPart("file") MultipartFile file,
                                         @RequestParam(required = false) UUID onBehalfOfMembershipId,
                                         HttpServletRequest httpRequest) {
        return service.uploadFile(communityId, user.id(), permitId, file, onBehalfOfMembershipId, httpRequest);
    }

    @GetMapping("/{permitId}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable UUID communityId, @PathVariable UUID permitId,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        WorkPermitService.FileDownload file = service.downloadFile(communityId, user.id(), permitId);
        String fileName = file.fileName() == null ? "permiso" : file.fileName();
        String contentType = file.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.contentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(file.content().length)
                .body(file.content());
    }

    @DeleteMapping("/{permitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID communityId, @PathVariable UUID permitId,
                       @AuthenticationPrincipal AuthenticatedUser user,
                       HttpServletRequest httpRequest) {
        service.delete(communityId, user.id(), permitId, httpRequest);
    }
}

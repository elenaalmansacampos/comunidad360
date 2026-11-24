package com.comunidad360.documents;

import com.comunidad360.documents.dto.DocumentDetailResponse;
import com.comunidad360.documents.dto.DocumentResponse;
import com.comunidad360.documents.dto.UpdateDocumentRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentResponse> list(@PathVariable UUID communityId,
                                       @RequestParam(required = false) DocumentCategory category,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return documentService.list(communityId, user.id(), category);
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> create(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam DocumentCategory category,
            @RequestParam DocumentVisibility visibility,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.create(communityId, user.id(), title, description,
                        category, visibility, file, httpRequest));
    }

    @GetMapping("/{documentId}")
    public DocumentDetailResponse detail(@PathVariable UUID communityId,
                                         @PathVariable UUID documentId,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return documentService.detail(communityId, user.id(), documentId);
    }

    @PatchMapping("/{documentId}")
    public DocumentResponse update(@PathVariable UUID communityId,
                                   @PathVariable UUID documentId,
                                   @AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody UpdateDocumentRequest request,
                                   HttpServletRequest httpRequest) {
        return documentService.update(communityId, user.id(), documentId, request, httpRequest);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> archive(@PathVariable UUID communityId,
                                        @PathVariable UUID documentId,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        HttpServletRequest httpRequest) {
        documentService.archive(communityId, user.id(), documentId, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{documentId}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanent(@PathVariable UUID communityId, @PathVariable UUID documentId,
                                @AuthenticationPrincipal AuthenticatedUser user,
                                HttpServletRequest httpRequest) {
        documentService.deletePermanent(communityId, user.id(), documentId, httpRequest);
    }

    @PostMapping("/{documentId}/versions")
    public ResponseEntity<DocumentResponse> addVersion(
            @PathVariable UUID communityId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.addVersion(communityId, user.id(), documentId, file, httpRequest));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID communityId,
                                             @PathVariable UUID documentId,
                                             @RequestParam(required = false) UUID versionId,
                                             @AuthenticationPrincipal AuthenticatedUser user,
                                             HttpServletRequest httpRequest) {
        DocumentService.DownloadFile file = documentService.download(
                communityId, user.id(), documentId, versionId, httpRequest);
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

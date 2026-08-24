package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.AttachmentResponse;
import com.helpdesk.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public List<AttachmentResponse> list(@PathVariable Long ticketId,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        return attachmentService.list(principal, ticketId);
    }

    @PostMapping
    public AttachmentResponse upload(@PathVariable Long ticketId,
                                     @RequestParam("file") MultipartFile file,
                                     @AuthenticationPrincipal JwtPrincipal principal) {
        return attachmentService.store(principal, ticketId, file);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long ticketId,
                                             @PathVariable Long attachmentId,
                                             @AuthenticationPrincipal JwtPrincipal principal) {
        AttachmentService.ResolvedAttachment resolved =
                attachmentService.resolveForDownload(principal, ticketId, attachmentId);
        String encodedName = UriUtils.encode(resolved.meta().getFileName(), StandardCharsets.UTF_8);
        MediaType mediaType = resolved.meta().getContentType() != null
                ? MediaType.parseMediaType(resolved.meta().getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .body(resolved.resource());
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long ticketId,
                                       @PathVariable Long attachmentId,
                                       @AuthenticationPrincipal JwtPrincipal principal) {
        attachmentService.delete(principal, ticketId, attachmentId);
        return ResponseEntity.noContent().build();
    }
}

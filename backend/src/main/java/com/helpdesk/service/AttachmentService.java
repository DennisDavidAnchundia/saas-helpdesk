package com.helpdesk.service;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.AttachmentResponse;
import com.helpdesk.model.Attachment;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.repository.AttachmentRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class AttachmentService {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final Path uploadsRoot;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             UserRepository userRepository,
                             ChatService chatService,
                             @Value("${app.uploads.dir:./data/uploads}") String uploadsDir) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.chatService = chatService;
        this.uploadsRoot = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    /** Lista los adjuntos de un ticket (solo participantes del ticket). */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(JwtPrincipal principal, Long ticketId) {
        chatService.accessibleTicket(principal, ticketId);
        return attachmentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional
    public AttachmentResponse store(JwtPrincipal principal, Long ticketId, MultipartFile file) {
        Ticket ticket = chatService.accessibleTicket(principal, ticketId);
        validate(file);

        String safeName = sanitize(file.getOriginalFilename());
        User uploader = userRepository.getReferenceById(principal.getUserId());

        Attachment attachment = new Attachment();
        attachment.setTenant(ticket.getTenant());
        attachment.setTicket(ticket);
        attachment.setUploader(uploader);
        attachment.setFileName(safeName);
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(file.getSize());

        Attachment saved = attachmentRepository.save(attachment);
        Path target = resolveDiskPath(ticket, saved.getId(), safeName);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            // Si el archivo no se pudo guardar, no dejamos metadata huerfana
            attachmentRepository.delete(saved);
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }
        return AttachmentResponse.from(saved);
    }

    /** Devuelve el archivo del disco tras validar participacion. */
    @Transactional(readOnly = true)
    public ResolvedAttachment resolveForDownload(JwtPrincipal principal, Long ticketId, Long attachmentId) {
        chatService.accessibleTicket(principal, ticketId);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> a.getTicket().getId().equals(ticketId))
                .orElseThrow(() -> new IllegalArgumentException("Adjunto no encontrado"));
        Path path = resolveDiskPath(attachment.getTicket(), attachment.getId(), attachment.getFileName());
        if (!Files.exists(path)) {
            throw new IllegalStateException("El archivo ya no existe en el servidor");
        }
        return new ResolvedAttachment(attachment, new FileSystemResource(path));
    }

    /** Solo quien lo subio o un ADMIN puede borrarlo. */
    @Transactional
    public void delete(JwtPrincipal principal, Long ticketId, Long attachmentId) {
        chatService.accessibleTicket(principal, ticketId);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> a.getTicket().getId().equals(ticketId))
                .orElseThrow(() -> new IllegalArgumentException("Adjunto no encontrado"));

        boolean isUploader = attachment.getUploader() != null
                && attachment.getUploader().getId().equals(principal.getUserId());
        if (!isUploader && !"ADMIN".equals(principal.getRole())) {
            throw new IllegalArgumentException("Solo quien subio el archivo o un admin puede borrarlo");
        }

        attachmentRepository.delete(attachment);
        Path path = resolveDiskPath(attachment.getTicket(), attachment.getId(), attachment.getFileName());
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // La metadata ya se borro; el archivo suelto es inofensivo
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo esta vacio");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo supera el limite de 10 MB");
        }
    }

    /** Quita rutas y caracteres problematicos del nombre original. */
    private String sanitize(String original) {
        String name = (original == null || original.isBlank()) ? "archivo" : original;
        name = Path.of(name).getFileName().toString().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return name.isEmpty() ? "archivo" : name;
    }

    /** Layout en disco determinista: {uploads}/{tenant}/{ticket}/{attachmentId}_{nombre}. */
    private Path resolveDiskPath(Ticket ticket, Long attachmentId, String fileName) {
        return uploadsRoot
                .resolve(String.valueOf(ticket.getTenant().getId()))
                .resolve(String.valueOf(ticket.getId()))
                .resolve(attachmentId + "_" + fileName)
                .normalize();
    }

    public record ResolvedAttachment(Attachment meta, FileSystemResource resource) {}
}

package com.helpdesk.dto;

import com.helpdesk.model.Attachment;

import java.time.LocalDateTime;

public class AttachmentResponse {

    private Long id;
    private String fileName;
    private String contentType;
    private long sizeBytes;
    private String uploaderName;
    private LocalDateTime createdAt;

    public static AttachmentResponse from(Attachment attachment) {
        AttachmentResponse response = new AttachmentResponse();
        response.id = attachment.getId();
        response.fileName = attachment.getFileName();
        response.contentType = attachment.getContentType();
        response.sizeBytes = attachment.getSizeBytes();
        response.uploaderName = attachment.getUploader() != null ? attachment.getUploader().getFullName() : null;
        response.createdAt = attachment.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

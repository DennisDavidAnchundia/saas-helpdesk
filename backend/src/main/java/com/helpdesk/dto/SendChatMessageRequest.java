package com.helpdesk.dto;

import jakarta.validation.constraints.Size;

public class SendChatMessageRequest {

    /**
     * Opcional SI el mensaje trae attachmentId (puede mandarse solo un archivo).
     * La regla "texto o adjunto, al menos uno" se valida en ChatService.
     */
    @Size(max = 4000, message = "El mensaje no puede superar 4000 caracteres")
    private String content;

    /** Id de un archivo ya subido a este ticket via POST /tickets/{id}/attachments. */
    private Long attachmentId;

    public SendChatMessageRequest() {}

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
}

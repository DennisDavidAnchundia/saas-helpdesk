package com.helpdesk.dto;

import com.helpdesk.model.Message;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private Long id;
    private Long ticketId;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
    private AttachmentResponse attachment;

    public ChatMessageResponse() {}

    public static ChatMessageResponse from(Message m) {
        ChatMessageResponse r = new ChatMessageResponse();
        r.id = m.getId();
        r.ticketId = m.getTicket() != null ? m.getTicket().getId() : null;
        r.senderId = m.getSender() != null ? m.getSender().getId() : null;
        r.senderName = m.getSender() != null ? m.getSender().getFullName() : null;
        r.content = m.getContent();
        r.sentAt = m.getCreatedAt();
        // Se accede DENTRO de la transaccion (attachment es LAZY)
        r.attachment = m.getAttachment() != null ? AttachmentResponse.from(m.getAttachment()) : null;
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public AttachmentResponse getAttachment() { return attachment; }
    public void setAttachment(AttachmentResponse attachment) { this.attachment = attachment; }
}

package com.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendChatMessageRequest {

    @NotBlank(message = "El mensaje no puede estar vacio")
    @Size(max = 4000, message = "El mensaje no puede superar 4000 caracteres")
    private String content;

    public SendChatMessageRequest() {}

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

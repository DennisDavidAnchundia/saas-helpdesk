package com.helpdesk.dto;

import com.helpdesk.model.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateTicketRequest {

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 255, message = "El titulo no puede superar 255 caracteres")
    private String title;

    @NotBlank(message = "La descripcion es obligatoria")
    private String description;

    private TicketPriority priority;

    public CreateTicketRequest() {}

    public CreateTicketRequest(String title, String description, TicketPriority priority) {
        this.title = title;
        this.description = description;
        this.priority = priority;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
}

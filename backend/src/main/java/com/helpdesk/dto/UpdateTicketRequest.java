package com.helpdesk.dto;

import com.helpdesk.model.enums.TicketPriority;
import jakarta.validation.constraints.Size;

public class UpdateTicketRequest {

    @Size(max = 255, message = "El titulo no puede superar 255 caracteres")
    private String title;

    private String description;

    private TicketPriority priority;

    public UpdateTicketRequest() {}

    public UpdateTicketRequest(String title, String description, TicketPriority priority) {
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

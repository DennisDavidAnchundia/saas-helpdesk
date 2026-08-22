package com.helpdesk.dto;

import jakarta.validation.constraints.Size;

public class UpdateArticleRequest {

    @Size(max = 255, message = "El titulo no puede superar 255 caracteres")
    private String title;

    private String content;

    @Size(max = 100, message = "La categoria no puede superar 100 caracteres")
    private String category;

    private Boolean isPublished;

    public UpdateArticleRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }
}

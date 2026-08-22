package com.helpdesk.dto;

import com.helpdesk.model.Article;

import java.time.LocalDateTime;

public class ArticleResponse {

    private Long id;
    private String title;
    private String content;
    private String category;
    private boolean isPublished;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ArticleResponse() {}

    public static ArticleResponse from(Article a) {
        ArticleResponse r = new ArticleResponse();
        r.id = a.getId();
        r.title = a.getTitle();
        r.content = a.getContent();
        r.category = a.getCategory();
        r.isPublished = a.isPublished();
        r.authorId = a.getAuthor() != null ? a.getAuthor().getId() : null;
        r.authorName = a.getAuthor() != null ? a.getAuthor().getFullName() : null;
        r.createdAt = a.getCreatedAt();
        r.updatedAt = a.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean getIsPublished() { return isPublished; }
    public void setIsPublished(boolean isPublished) { this.isPublished = isPublished; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

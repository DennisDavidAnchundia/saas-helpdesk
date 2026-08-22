package com.helpdesk.service;

import com.helpdesk.dto.ArticleResponse;
import com.helpdesk.dto.CreateArticleRequest;
import com.helpdesk.dto.UpdateArticleRequest;
import com.helpdesk.model.Article;
import com.helpdesk.model.User;
import com.helpdesk.repository.ArticleRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public ArticleService(ArticleRepository articleRepository, UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ArticleResponse create(Long authorId, CreateArticleRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Article article = new Article();
        article.setTenant(author.getTenant());
        article.setAuthor(author);
        article.setTitle(request.getTitle().trim());
        article.setContent(request.getContent().trim());
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            article.setCategory(request.getCategory().trim());
        }
        if (request.getIsPublished() != null) {
            article.setIsPublished(request.getIsPublished());
        }

        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> listForTenant(Long tenantId, String role, String q) {
        boolean staff = !"CUSTOMER".equals(role);
        boolean hasQuery = q != null && !q.isBlank();

        List<Article> articles;
        if (staff) {
            articles = hasQuery
                    ? articleRepository.searchAll(tenantId, q.trim())
                    : articleRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        } else {
            articles = hasQuery
                    ? articleRepository.searchPublished(tenantId, q.trim())
                    : articleRepository.findByTenantIdAndIsPublishedTrueOrderByCreatedAtDesc(tenantId);
        }
        return articles.stream()
                .map(ArticleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse getForTenant(Long tenantId, Long articleId, String role) {
        Article article = getEntity(tenantId, articleId);
        if ("CUSTOMER".equals(role) && !article.isPublished()) {
            throw new IllegalArgumentException("Articulo no encontrado");
        }
        return ArticleResponse.from(article);
    }

    @Transactional
    public ArticleResponse update(Long tenantId, Long articleId, UpdateArticleRequest request) {
        Article article = getEntity(tenantId, articleId);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            article.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            article.setContent(request.getContent().trim());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            article.setCategory(request.getCategory().trim());
        }
        if (request.getIsPublished() != null) {
            article.setIsPublished(request.getIsPublished());
        }
        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional
    public void delete(Long tenantId, Long articleId) {
        Article article = getEntity(tenantId, articleId);
        articleRepository.delete(article);
    }

    private Article getEntity(Long tenantId, Long articleId) {
        return articleRepository.findByIdAndTenantId(articleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Articulo no encontrado"));
    }
}

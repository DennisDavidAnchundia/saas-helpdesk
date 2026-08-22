package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.ArticleResponse;
import com.helpdesk.dto.CreateArticleRequest;
import com.helpdesk.dto.UpdateArticleRequest;
import com.helpdesk.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody CreateArticleRequest request,
                                                  @AuthenticationPrincipal JwtPrincipal principal) {
        ArticleResponse response = articleService.create(principal.getUserId(), request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public List<ArticleResponse> list(@RequestParam(required = false) String q,
                                      @AuthenticationPrincipal JwtPrincipal principal) {
        return articleService.listForTenant(principal.getTenantId(), principal.getRole(), q);
    }

    @GetMapping("/{id}")
    public ArticleResponse get(@PathVariable Long id,
                               @AuthenticationPrincipal JwtPrincipal principal) {
        return articleService.getForTenant(principal.getTenantId(), id, principal.getRole());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ArticleResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateArticleRequest request,
                                  @AuthenticationPrincipal JwtPrincipal principal) {
        return articleService.update(principal.getTenantId(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal JwtPrincipal principal) {
        articleService.delete(principal.getTenantId(), id);
        return ResponseEntity.noContent().build();
    }
}

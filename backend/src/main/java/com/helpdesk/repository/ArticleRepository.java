package com.helpdesk.repository;

import com.helpdesk.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findByTenantIdAndIsPublishedTrueOrderByCreatedAtDesc(Long tenantId);

    List<Article> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    Optional<Article> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT a FROM Article a WHERE a.tenant.id = :tenantId AND a.isPublished = true " +
            "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "   OR LOWER(a.content) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Article> searchPublished(@Param("tenantId") Long tenantId, @Param("q") String q);

    @Query("SELECT a FROM Article a WHERE a.tenant.id = :tenantId " +
            "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "   OR LOWER(a.content) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Article> searchAll(@Param("tenantId") Long tenantId, @Param("q") String q);
}

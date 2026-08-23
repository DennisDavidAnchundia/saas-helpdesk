package com.helpdesk.repository;

import com.helpdesk.model.Ticket;
import com.helpdesk.model.enums.TicketStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByIdAndTenantId(Long id, Long tenantId);

    List<Ticket> findByTenantIdAndAgentId(Long tenantId, Long agentId);

    List<Ticket> findByTenantIdAndCustomerId(Long tenantId, Long customerId);

    long countByTenantIdAndStatus(Long tenantId, String status);

    long countByAgentIdAndStatusIn(Long agentId, Collection<TicketStatus> statuses);

    long countByTenantIdAndSlaDueAtBeforeAndStatusIn(Long tenantId,
                                                     LocalDateTime before,
                                                     Collection<TicketStatus> statuses);

    @Query("SELECT t.status, COUNT(t) FROM Ticket t WHERE t.tenant.id = :tenantId GROUP BY t.status")
    List<Object[]> countByStatus(@Param("tenantId") Long tenantId);

    @Query("SELECT t.createdAt, t.resolvedAt FROM Ticket t " +
           "WHERE t.tenant.id = :tenantId AND t.resolvedAt IS NOT NULL")
    List<Object[]> resolutionPairsByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT t.createdAt, t.firstResponseAt FROM Ticket t " +
           "WHERE t.tenant.id = :tenantId AND t.firstResponseAt IS NOT NULL")
    List<Object[]> firstResponsePairsByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT t.agent.fullName, COUNT(t) FROM Ticket t WHERE t.tenant.id = :tenantId AND t.agent IS NOT NULL " +
           "GROUP BY t.agent.id, t.agent.fullName ORDER BY COUNT(t) DESC")
    List<Object[]> countByAgent(@Param("tenantId") Long tenantId, Pageable pageable);
}

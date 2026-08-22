package com.helpdesk.repository;

import com.helpdesk.model.Ticket;
import com.helpdesk.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<Ticket> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, String status);

    Optional<Ticket> findByIdAndTenantId(Long id, Long tenantId);

    List<Ticket> findByTenantIdAndAgentId(Long tenantId, Long agentId);

    List<Ticket> findByTenantIdAndCustomerId(Long tenantId, Long customerId);

    long countByTenantIdAndStatus(Long tenantId, String status);

    long countByAgentIdAndStatusIn(Long agentId, Collection<TicketStatus> statuses);
}

package com.helpdesk.repository;

import com.helpdesk.model.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {

    Optional<MessageRead> findByTicketIdAndUserId(Long ticketId, Long userId);

    /**
     * Mensajes de otros usuarios escritos DESPUES de la ultima lectura de cada ticket.
     * Solo cuenta tickets donde el usuario participa (AGENT asignado / CUSTOMER dueño;
     * ADMIN ve todo el tenant).
     */
    @Query(value = """
            SELECT m.ticket_id, COUNT(*)
            FROM messages m
            JOIN tickets t ON t.id = m.ticket_id
            LEFT JOIN message_reads r ON r.ticket_id = m.ticket_id AND r.user_id = :userId
            WHERE m.tenant_id = :tenantId
              AND m.sender_id <> :userId
              AND (r.last_read_at IS NULL OR m.created_at > r.last_read_at)
              AND (
                    :role = 'ADMIN'
                 OR (:role = 'AGENT' AND t.agent_id = :userId)
                 OR (:role = 'CUSTOMER' AND t.customer_id = :userId)
              )
            GROUP BY m.ticket_id
            """, nativeQuery = true)
    List<Object[]> countUnreadByUser(@Param("userId") Long userId,
                                     @Param("tenantId") Long tenantId,
                                     @Param("role") String role);
}

package com.helpdesk.repository;

import com.helpdesk.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    /**
     * Pagina desde los mas recientes (page 0). El desempate por id DESC evita
     * orden inestable cuando dos mensajes comparten createdAt.
     */
    Page<Message> findByTicketIdOrderByCreatedAtDescIdDesc(Long ticketId, Pageable pageable);
}

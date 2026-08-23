package com.helpdesk.repository;

import com.helpdesk.model.Ticket;
import com.helpdesk.model.enums.TicketPriority;
import com.helpdesk.model.enums.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class TicketSpecifications {

    private TicketSpecifications() {}

    /** Filtros opcionales: los null se ignoran en la consulta dinamica. */
    public static Specification<Ticket> withFilters(Long tenantId,
                                                    TicketStatus status,
                                                    TicketPriority priority,
                                                    Long agentId,
                                                    Long customerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (agentId != null) {
                predicates.add(cb.equal(root.get("agent").get("id"), agentId));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

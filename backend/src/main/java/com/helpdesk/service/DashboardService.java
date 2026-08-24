package com.helpdesk.service;

import com.helpdesk.dto.DashboardSummaryResponse;
import com.helpdesk.dto.TrendPointResponse;
import com.helpdesk.model.enums.TicketStatus;
import com.helpdesk.repository.TicketRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

    private static final List<TicketStatus> ACTIVE_STATUSES =
            List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.REOPENED);

    private final TicketRepository ticketRepository;

    public DashboardService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(Long tenantId) {
        DashboardSummaryResponse response = new DashboardSummaryResponse();

        long total = 0;
        for (TicketStatus status : TicketStatus.values()) {
            response.getTicketsByStatus().put(status.name(), 0L);
        }
        for (Object[] row : ticketRepository.countByStatus(tenantId)) {
            TicketStatus status = (TicketStatus) row[0];
            long count = (Long) row[1];
            response.getTicketsByStatus().put(status.name(), count);
            total += count;
        }
        response.setTotalTickets(total);

        response.setAvgResolutionSeconds(averageSeconds(ticketRepository.resolutionPairsByTenant(tenantId)));
        response.setAvgFirstResponseSeconds(averageSeconds(ticketRepository.firstResponsePairsByTenant(tenantId)));
        response.setSlaBreachedCount(
                ticketRepository.countByTenantIdAndSlaDueAtBeforeAndStatusIn(
                        tenantId, LocalDateTime.now(), ACTIVE_STATUSES));

        for (Object[] row : ticketRepository.countByAgent(tenantId, PageRequest.of(0, 5))) {
            DashboardSummaryResponse.AgentStat stat = new DashboardSummaryResponse.AgentStat();
            stat.setAgentName((String) row[0]);
            stat.setAssignedTickets((Long) row[1]);
            response.getTopAgents().add(stat);
        }

        return response;
    }

    private Double averageSeconds(List<Object[]> pairs) {
        if (pairs.isEmpty()) {
            return null;
        }
        long total = 0;
        for (Object[] pair : pairs) {
            total += Duration.between((LocalDateTime) pair[0], (LocalDateTime) pair[1]).getSeconds();
        }
        return (double) total / pairs.size();
    }

    /** Serie diaria creados/resueltos de los ultimos 14 dias (incluye hoy). */
    @Transactional(readOnly = true)
    public List<TrendPointResponse> trend(Long tenantId) {
        return ticketRepository.dailyTrendRaw(tenantId).stream()
                .map(row -> new TrendPointResponse(
                        LocalDate.parse((String) row[0]),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()))
                .toList();
    }
}

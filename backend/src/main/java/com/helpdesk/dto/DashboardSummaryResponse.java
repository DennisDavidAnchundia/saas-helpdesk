package com.helpdesk.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardSummaryResponse {

    private long totalTickets;
    private Map<String, Long> ticketsByStatus = new LinkedHashMap<>();
    private Double avgResolutionSeconds;
    private Double avgFirstResponseSeconds;
    private long slaBreachedCount;
    private List<AgentStat> topAgents = new ArrayList<>();

    public long getTotalTickets() { return totalTickets; }
    public void setTotalTickets(long totalTickets) { this.totalTickets = totalTickets; }

    public Map<String, Long> getTicketsByStatus() { return ticketsByStatus; }
    public void setTicketsByStatus(Map<String, Long> ticketsByStatus) { this.ticketsByStatus = ticketsByStatus; }

    public Double getAvgResolutionSeconds() { return avgResolutionSeconds; }
    public void setAvgResolutionSeconds(Double avgResolutionSeconds) { this.avgResolutionSeconds = avgResolutionSeconds; }

    public Double getAvgFirstResponseSeconds() { return avgFirstResponseSeconds; }
    public void setAvgFirstResponseSeconds(Double avgFirstResponseSeconds) { this.avgFirstResponseSeconds = avgFirstResponseSeconds; }

    public long getSlaBreachedCount() { return slaBreachedCount; }
    public void setSlaBreachedCount(long slaBreachedCount) { this.slaBreachedCount = slaBreachedCount; }

    public List<AgentStat> getTopAgents() { return topAgents; }
    public void setTopAgents(List<AgentStat> topAgents) { this.topAgents = topAgents; }

    public static class AgentStat {

        private String agentName;
        private long assignedTickets;

        public String getAgentName() { return agentName; }
        public void setAgentName(String agentName) { this.agentName = agentName; }

        public long getAssignedTickets() { return assignedTickets; }
        public void setAssignedTickets(long assignedTickets) { this.assignedTickets = assignedTickets; }
    }
}

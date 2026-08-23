package com.helpdesk.dto;

import com.helpdesk.model.Subscription;
import com.helpdesk.model.enums.SubscriptionPlan;

public class SubscriptionResponse {

    private SubscriptionPlan plan;
    private String status;
    private int ticketsUsed;
    private int ticketsLimit;
    private String currentPeriodEnd;

    public static SubscriptionResponse from(Subscription subscription) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.plan = subscription.getPlan();
        response.status = subscription.getStatus().name();
        response.ticketsUsed = subscription.getTicketsUsed();
        response.ticketsLimit = subscription.getTicketsLimit();
        response.currentPeriodEnd = subscription.getCurrentPeriodEnd() != null
                ? subscription.getCurrentPeriodEnd().toString()
                : null;
        return response;
    }

    public SubscriptionPlan getPlan() { return plan; }
    public void setPlan(SubscriptionPlan plan) { this.plan = plan; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTicketsUsed() { return ticketsUsed; }
    public void setTicketsUsed(int ticketsUsed) { this.ticketsUsed = ticketsUsed; }

    public int getTicketsLimit() { return ticketsLimit; }
    public void setTicketsLimit(int ticketsLimit) { this.ticketsLimit = ticketsLimit; }

    public String getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(String currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
}

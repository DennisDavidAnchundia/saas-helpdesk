package com.helpdesk.model.enums;

public enum SubscriptionPlan {
    FREE(100),
    PRO(5000),
    ENTERPRISE(Integer.MAX_VALUE);

    private final int ticketLimit;

    SubscriptionPlan(int ticketLimit) {
        this.ticketLimit = ticketLimit;
    }

    public int getTicketLimit() {
        return ticketLimit;
    }
}

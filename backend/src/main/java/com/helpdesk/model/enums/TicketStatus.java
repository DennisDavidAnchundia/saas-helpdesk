package com.helpdesk.model.enums;

public enum TicketStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED, REOPENED;

    public boolean canTransitionTo(TicketStatus target) {
        if (target == this) return false;
        return switch (this) {
            case OPEN        -> target == IN_PROGRESS || target == RESOLVED;
            case IN_PROGRESS -> target == RESOLVED;
            case RESOLVED    -> target == CLOSED || target == REOPENED;
            case CLOSED      -> target == REOPENED;
            case REOPENED    -> target == IN_PROGRESS || target == RESOLVED;
        };
    }
}

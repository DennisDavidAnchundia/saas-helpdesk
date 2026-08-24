package com.helpdesk.dto;

import java.time.LocalDate;

/** Un dia de la serie de tendencia del dashboard: creados vs resueltos. */
public class TrendPointResponse {

    private LocalDate date;
    private long created;
    private long resolved;

    public TrendPointResponse() {}

    public TrendPointResponse(LocalDate date, long created, long resolved) {
        this.date = date;
        this.created = created;
        this.resolved = resolved;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }

    public long getResolved() { return resolved; }
    public void setResolved(long resolved) { this.resolved = resolved; }
}

-- V3: Add SLA tracking fields to tickets

ALTER TABLE tickets ADD COLUMN closed_at TIMESTAMP;
ALTER TABLE tickets ADD COLUMN sla_due_at TIMESTAMP;

CREATE INDEX idx_tickets_sla_due ON tickets(sla_due_at);

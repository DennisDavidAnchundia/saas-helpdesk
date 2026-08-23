-- V4: SLA de resolucion configurable por tenant (horas maximas por prioridad)
-- Valores iniciales = politica estandar que vivia hardcodeada en TicketService

ALTER TABLE tenants ADD COLUMN sla_urgent_hours INT NOT NULL DEFAULT 4;
ALTER TABLE tenants ADD COLUMN sla_high_hours INT NOT NULL DEFAULT 8;
ALTER TABLE tenants ADD COLUMN sla_medium_hours INT NOT NULL DEFAULT 24;
ALTER TABLE tenants ADD COLUMN sla_low_hours INT NOT NULL DEFAULT 72;

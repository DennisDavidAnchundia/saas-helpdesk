-- Normaliza los timestamps a UTC.
--
-- Historia: las columnas son TIMESTAMP WITHOUT TIME ZONE y JPA escribia hora
-- de pared del host (America/Guayaquil, UTC-5). Ahora la JVM se fija a UTC en
-- SaasHelpdeskApplication.main, asi que este script convierte lo viejo al
-- nuevo convenio: un valor naive "15:00" guardado como hora Guayaquil pasa a
-- guardarse como "20:00" UTC. El doble AT TIME ZONE hace justamente eso:
-- 1) interpreta el naive como hora Guayaquil -> timestamptz
-- 2) lo vuelve naive pero en hora UTC.
-- Sobre una BD vacia (deploy fresco o tests H2 sin Flyway) no afecta nada.

UPDATE tenants SET created_at = created_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                   updated_at = updated_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC';

UPDATE users SET created_at = created_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                 updated_at = updated_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC';

UPDATE tickets SET first_response_at = first_response_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                   resolved_at = resolved_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                   closed_at = closed_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                   sla_due_at = sla_due_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                   created_at = created_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                   updated_at = updated_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC';

UPDATE articles SET created_at = created_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                    updated_at = updated_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC';

UPDATE subscriptions SET current_period_start = current_period_start AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                         current_period_end = current_period_end AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                         created_at = created_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC',
                         updated_at = updated_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC';

UPDATE messages SET created_at = created_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC';

UPDATE message_reads SET last_read_at = last_read_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC';

UPDATE attachments SET created_at = created_at AT TIME ZONE 'America/Guayaquil' AT TIME ZONE 'UTC';

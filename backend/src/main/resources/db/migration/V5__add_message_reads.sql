-- Marca de "ultimo mensaje leido" por usuario y ticket para calcular no leidos.
CREATE TABLE message_reads (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    last_read_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_message_reads_ticket_user UNIQUE (ticket_id, user_id)
);

CREATE INDEX idx_message_reads_user ON message_reads(user_id);

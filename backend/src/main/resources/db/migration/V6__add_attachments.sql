-- Archivos adjuntos de un ticket. El contenido vive en disco (app.uploads.dir);
-- aqui solo metadata.
CREATE TABLE attachments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    ticket_id BIGINT NOT NULL REFERENCES tickets(id),
    uploader_id BIGINT NOT NULL REFERENCES users(id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attachments_ticket ON attachments(ticket_id);

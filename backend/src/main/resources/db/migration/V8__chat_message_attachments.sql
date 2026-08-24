-- Adjuntos en el chat: un mensaje puede referenciar un archivo ya subido
-- al ticket (tabla attachments, creada en V6). Nullable porque casi todos
-- los mensajes son solo texto. La validacion de que el adjunto pertenezca
-- al mismo ticket y sea del remitente vive en ChatService.send.

ALTER TABLE messages ADD COLUMN attachment_id BIGINT REFERENCES attachments(id);

-- V15__audit_log.sql
-- Log basico de acoes sensiveis (create/update/delete em account, category,
-- bill, checklist_item) -- essencial pra rastrear se um lancamento errado
-- foi um humano ou uma integracao automatizada (ex: futura importacao via
-- WhatsApp) que causou.

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account (id_user) ON DELETE CASCADE,
    user_email VARCHAR(255) NOT NULL,
    action VARCHAR(20) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    details VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_user_id ON audit_log (user_id);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);

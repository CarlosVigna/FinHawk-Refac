-- V3__checklist_item_cascade.sql
-- Adiciona ON DELETE CASCADE na FK checklist_item.account_id.
-- Elimina o bug G-01: DELETE em account com checklist_items associados falha com FK violation.
-- Consistente com fk_category_account que ja tem ON DELETE CASCADE.
--
-- O nome da constraint existente e desconhecido (criada por Hibernate ddl-auto=update).
-- O bloco DO faz lookup via information_schema e dropa a constraint pelo nome real antes de recriar.

DO $$
DECLARE
    v_constraint TEXT;
BEGIN
    SELECT tc.constraint_name INTO v_constraint
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name
       AND tc.table_schema   = kcu.table_schema
    WHERE tc.constraint_type = 'FOREIGN KEY'
      AND tc.table_schema    = 'public'
      AND tc.table_name      = 'checklist_item'
      AND kcu.column_name    = 'account_id';

    IF v_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE checklist_item DROP CONSTRAINT %I', v_constraint);
    END IF;
END $$;

ALTER TABLE checklist_item
    ADD CONSTRAINT fk_checklist_item_account
    FOREIGN KEY (account_id) REFERENCES account(id_account) ON DELETE CASCADE;

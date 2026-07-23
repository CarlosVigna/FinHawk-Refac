-- V12__bill_account_cascade.sql
-- Adiciona ON DELETE CASCADE na FK bill.account_id.
-- Mesma classe de bug que o G-01 corrigido na V3 (checklist_item): hoje
-- DELETE em account com bills associados so funciona porque
-- Account.bills tem cascade=CascadeType.ALL no JPA (Hibernate apaga os
-- bills antes de apagar a account). Se essa anotacao for removida num
-- refactor futuro, o DELETE passa a falhar com FK violation. Adicionando
-- o cascade tambem no banco deixa consistente com fk_category_account e
-- fk_checklist_item_account (ambas ja ON DELETE CASCADE), como o
-- comentario da V3 ja dizia ser a intencao original.
--
-- O nome da constraint existente e desconhecido (criada por Hibernate
-- ddl-auto=update). Mesmo padrao de lookup via information_schema da V3.

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
      AND tc.table_name      = 'bill'
      AND kcu.column_name    = 'account_id';

    IF v_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE bill DROP CONSTRAINT %I', v_constraint);
    END IF;
END $$;

ALTER TABLE bill
    ADD CONSTRAINT fk_bill_account
    FOREIGN KEY (account_id) REFERENCES account(id_account) ON DELETE CASCADE;

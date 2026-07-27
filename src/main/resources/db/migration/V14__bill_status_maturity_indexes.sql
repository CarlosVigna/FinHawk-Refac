-- V14__bill_status_maturity_indexes.sql
-- account_id e category_id ja tem indice (V6). As duas queries mais
-- frequentes do dia a dia sao "contas a pagar/receber de uma conta" (filtra
-- account_id + status) e "lancamentos de uma conta num periodo" (filtra
-- account_id + maturity) -- indice composto serve essas duas consultas
-- diretamente em vez de so usar o indice de account_id e filtrar o resto
-- em memoria.

CREATE INDEX IF NOT EXISTS idx_bill_account_id_status
    ON bill (account_id, status);

CREATE INDEX IF NOT EXISTS idx_bill_account_id_maturity
    ON bill (account_id, maturity);

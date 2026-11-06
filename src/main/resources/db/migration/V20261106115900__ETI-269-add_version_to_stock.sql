-- A entidade Stock usa @Version (lock otimista) desde o ETI-158,
-- mas a coluna nunca foi criada. Necessária para a baixa de estoque no checkout.
ALTER TABLE stock
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

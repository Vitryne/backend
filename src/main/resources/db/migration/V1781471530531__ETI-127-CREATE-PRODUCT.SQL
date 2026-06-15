CREATE TABLE produto (
                         id                  BIGSERIAL PRIMARY KEY,
                         nome                VARCHAR(255) NOT NULL,
                         descricao           TEXT,
                         preco               NUMERIC(10,2) NOT NULL,
                         preco_promocional   NUMERIC(10,2),
                         tipo                VARCHAR(50) NOT NULL,
                         cor                 VARCHAR(50),
                         avaliacao           REAL DEFAULT 0,
                         status              VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
                         fotos_urls          TEXT[],

                         CONSTRAINT chk_produto_status CHECK (status IN ('ATIVO', 'INATIVO')),
                         CONSTRAINT chk_produto_avaliacao CHECK (avaliacao >= 0 AND avaliacao <= 5),
                         CONSTRAINT chk_produto_preco_promocional CHECK (preco_promocional IS NULL OR preco_promocional <= preco)
);
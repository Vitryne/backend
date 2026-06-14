CREATE TABLE estoque (
                         id          BIGSERIAL PRIMARY KEY,
                         produto_id  BIGINT NOT NULL,
                         tamanho     VARCHAR(10) NOT NULL,
                         quantidade  INTEGER NOT NULL DEFAULT 0,

                         CONSTRAINT fk_estoque_produto FOREIGN KEY (produto_id) REFERENCES produto(id) ON DELETE CASCADE,
                         CONSTRAINT uq_estoque_produto_tamanho UNIQUE (produto_id, tamanho),
                         CONSTRAINT chk_estoque_quantidade CHECK (quantidade >= 0)
);

CREATE INDEX idx_estoque_produto_id ON estoque(produto_id);
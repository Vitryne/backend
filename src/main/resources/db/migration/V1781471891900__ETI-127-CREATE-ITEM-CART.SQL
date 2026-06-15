CREATE TABLE item_carrinho (
                               id              BIGSERIAL PRIMARY KEY,
                               carrinho_id     BIGINT NOT NULL,
                               estoque_id      BIGINT NOT NULL,
                               quantidade      INTEGER NOT NULL,
                               preco_unitario  NUMERIC(10,2) NOT NULL,

                               CONSTRAINT fk_item_carrinho_carrinho FOREIGN KEY (carrinho_id) REFERENCES carrinho_compras(id) ON DELETE CASCADE,
                               CONSTRAINT fk_item_carrinho_estoque FOREIGN KEY (estoque_id) REFERENCES estoque(id),
                               CONSTRAINT chk_item_carrinho_quantidade CHECK (quantidade > 0),
                               CONSTRAINT uq_item_carrinho_estoque UNIQUE (carrinho_id, estoque_id)
);

CREATE INDEX idx_item_carrinho_carrinho_id ON item_carrinho(carrinho_id);
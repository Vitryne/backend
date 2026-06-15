CREATE TABLE carrinho_compras (
                                  id                      BIGSERIAL PRIMARY KEY,
                                  usuario_id              BIGINT NOT NULL,
                                  previsao_valor_total    NUMERIC(10,2) NOT NULL DEFAULT 0,
                                  atualizado_em           TIMESTAMP NOT NULL DEFAULT now(),

                                  CONSTRAINT fk_carrinho_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
                                  CONSTRAINT uq_carrinho_usuario UNIQUE (usuario_id)
);

CREATE INDEX idx_carrinho_usuario_id ON carrinho_compras(usuario_id);
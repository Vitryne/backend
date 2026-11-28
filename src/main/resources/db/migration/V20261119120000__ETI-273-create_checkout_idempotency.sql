-- Chave de idempotência do checkout: a mesma confirmação repetida devolve o
-- resultado já registrado, sem novo pagamento, pedido ou baixa de estoque.
-- A chave pertence ao cliente: a unicidade é por (user_id, idempotency_key).
CREATE TABLE IF NOT EXISTS checkout_idempotency (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    idempotency_key  VARCHAR(100) NOT NULL,
    request_hash     CHAR(64) NOT NULL,
    payment_id       BIGINT NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_checkout_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_checkout_idempotency_payment FOREIGN KEY (payment_id) REFERENCES payment(id),
    CONSTRAINT uq_checkout_idempotency_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT uq_checkout_idempotency_payment UNIQUE (payment_id)
);

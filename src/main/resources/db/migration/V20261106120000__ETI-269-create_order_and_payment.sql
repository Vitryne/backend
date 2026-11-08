CREATE TABLE IF NOT EXISTS payment (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    amount      NUMERIC(12,2) NOT NULL,
    method      VARCHAR(20) NOT NULL,
    result      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_payment_amount CHECK (amount >= 0),
    CONSTRAINT chk_payment_method CHECK (method IN ('CREDIT_CARD', 'DEBIT_CARD', 'PIX')),
    CONSTRAINT chk_payment_result CHECK (result IN ('APPROVED', 'DECLINED'))
);

CREATE INDEX IF NOT EXISTS idx_payment_user_id ON payment(user_id);

-- "order" é palavra reservada no PostgreSQL.
-- Pedido só existe com pagamento aprovado; pagamentos recusados ficam sem pedido.
CREATE TABLE IF NOT EXISTS orders (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    store_id      BIGINT NOT NULL,
    payment_id    BIGINT NOT NULL,
    status        VARCHAR(20) NOT NULL,
    total_amount  NUMERIC(12,2) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES store(id),
    CONSTRAINT fk_orders_payment FOREIGN KEY (payment_id) REFERENCES payment(id),
    CONSTRAINT uq_orders_payment UNIQUE (payment_id),
    CONSTRAINT chk_orders_status CHECK (status IN ('CONFIRMED', 'PREPARING')),
    CONSTRAINT chk_orders_total CHECK (total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_store_id ON orders(store_id);

CREATE TABLE IF NOT EXISTS order_item (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL,
    stock_id      BIGINT NOT NULL,
    product_name  VARCHAR(255) NOT NULL,
    size          VARCHAR(10) NOT NULL,
    quantity      INTEGER NOT NULL,
    unit_price    NUMERIC(12,2) NOT NULL,
    subtotal      NUMERIC(12,2) NOT NULL,

    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_stock FOREIGN KEY (stock_id) REFERENCES stock(id),
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT uq_order_item_stock UNIQUE (order_id, stock_id)
);

CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item(order_id);

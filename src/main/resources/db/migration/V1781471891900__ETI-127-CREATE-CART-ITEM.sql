CREATE TABLE IF NOT EXISTS cart_item (
                            id           BIGSERIAL PRIMARY KEY,
                            cart_id      BIGINT NOT NULL,
                            stock_id     BIGINT NOT NULL,
                            quantity     INTEGER NOT NULL,
                            unit_price   NUMERIC(10,2) NOT NULL,

                            CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
                            CONSTRAINT fk_cart_item_stock FOREIGN KEY (stock_id) REFERENCES stock(id),
                            CONSTRAINT chk_cart_item_quantity CHECK (quantity > 0),
                            CONSTRAINT uq_cart_item_stock UNIQUE (cart_id, stock_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_item_cart_id ON cart_item(cart_id);

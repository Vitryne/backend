CREATE TABLE IF NOT EXISTS item_cart (
                            id           BIGSERIAL PRIMARY KEY,
                            cart_id      BIGINT NOT NULL,
                            stock_id     BIGINT NOT NULL,
                            quantity     INTEGER NOT NULL,
                            unit_price   NUMERIC(10,2) NOT NULL,

                            CONSTRAINT fk_item_cart_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
                            CONSTRAINT fk_item_cart_stock FOREIGN KEY (stock_id) REFERENCES stock(id),
                            CONSTRAINT chk_item_cart_quantity CHECK (quantity > 0),
                            CONSTRAINT uq_item_cart_stock UNIQUE (cart_id, stock_id)
);

CREATE INDEX IF NOT EXISTS idx_item_cart_cart_id ON item_cart(cart_id);

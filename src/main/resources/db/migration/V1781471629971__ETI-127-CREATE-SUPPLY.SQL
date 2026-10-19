CREATE TABLE IF NOT EXISTS stock (
                        id          BIGSERIAL PRIMARY KEY,
                        product_id  BIGINT NOT NULL,
                        size        VARCHAR(10) NOT NULL,
                        quantity    INTEGER NOT NULL DEFAULT 0,

                        CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
                        CONSTRAINT uq_stock_product_size UNIQUE (product_id, size),
                        CONSTRAINT chk_stock_quantity CHECK (quantity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_stock_product_id ON stock(product_id);

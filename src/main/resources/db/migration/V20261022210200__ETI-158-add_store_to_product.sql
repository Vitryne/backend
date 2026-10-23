ALTER TABLE product
    ADD COLUMN store_id BIGINT NOT NULL;

ALTER TABLE product
    ADD CONSTRAINT fk_product_store
        FOREIGN KEY (store_id)
        REFERENCES store(id);

CREATE INDEX idx_product_store_id ON product(store_id);
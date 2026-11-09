-- Frete (zero no MVP) e cópia do endereço de entrega no momento da compra.
-- Colunas de endereço aceitam nulo apenas para pedidos anteriores a esta versão.
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS shipping_amount       NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS delivery_zip_code     VARCHAR(8),
    ADD COLUMN IF NOT EXISTS delivery_street       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS delivery_number       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS delivery_neighborhood VARCHAR(100),
    ADD COLUMN IF NOT EXISTS delivery_complement   VARCHAR(150),
    ADD COLUMN IF NOT EXISTS delivery_city         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS delivery_state        VARCHAR(2);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_shipping CHECK (shipping_amount >= 0);

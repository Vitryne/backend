-- ETI-409: ciclo de vida do pedido com aceite da loja e cancelamento.
-- Pedidos existentes (CONFIRMED/PREPARING) permanecem como estão.

ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_status;
ALTER TABLE orders ADD CONSTRAINT chk_orders_status CHECK (status IN (
    'AWAITING_ACCEPTANCE', 'CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP',
    'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELED'
));

ALTER TABLE orders ADD COLUMN IF NOT EXISTS canceled_by         VARCHAR(10);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(40);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS canceled_at         TIMESTAMPTZ;

ALTER TABLE orders ADD CONSTRAINT chk_orders_canceled_by
    CHECK (canceled_by IS NULL OR canceled_by IN ('CLIENT', 'STORE'));

-- Pedido cancelado sempre registra quem, por quê e quando; os demais não têm esses dados.
ALTER TABLE orders ADD CONSTRAINT chk_orders_cancellation CHECK (
    (status = 'CANCELED'
        AND canceled_by IS NOT NULL AND cancellation_reason IS NOT NULL AND canceled_at IS NOT NULL)
    OR (status <> 'CANCELED'
        AND canceled_by IS NULL AND cancellation_reason IS NULL AND canceled_at IS NULL)
);

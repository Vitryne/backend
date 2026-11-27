-- ETI-412: prazo de aceite do pedido e cancelamento automático pelo sistema.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS acceptance_deadline TIMESTAMPTZ;

-- Pedidos que já aguardam aceite ganham o prazo padrão a partir da criação.
UPDATE orders
SET acceptance_deadline = created_at + INTERVAL '30 minutes'
WHERE status = 'AWAITING_ACCEPTANCE' AND acceptance_deadline IS NULL;

-- Todo pedido aguardando aceite tem prazo.
ALTER TABLE orders ADD CONSTRAINT chk_orders_acceptance_deadline
    CHECK (status <> 'AWAITING_ACCEPTANCE' OR acceptance_deadline IS NOT NULL);

-- SYSTEM: cancelamento automático por falta de aceite no prazo.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_canceled_by;
ALTER TABLE orders ADD CONSTRAINT chk_orders_canceled_by
    CHECK (canceled_by IS NULL OR canceled_by IN ('CLIENT', 'STORE', 'SYSTEM'));

-- Busca da rotina de expiração: só os pedidos aguardando aceite, pelo prazo.
CREATE INDEX IF NOT EXISTS idx_orders_awaiting_acceptance_deadline
    ON orders (acceptance_deadline)
    WHERE status = 'AWAITING_ACCEPTANCE';
